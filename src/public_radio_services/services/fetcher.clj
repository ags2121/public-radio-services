(ns public-radio-services.services.fetcher
  (:require [org.httpkit.client :as httpkit]
            [clojure.core.async :refer [chan go >! <!]]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [clojure.data.xml :as xml]
            [clojure.data.xml.name :as name]
            [clojure.string :as str]))

(def NPR-API-KEY (env :npr-api-key))
(def NPR-ENDPOINT (str "https://api.npr.org/query?id=500005&profileTypeId=15"
                       "&apiKey=" NPR-API-KEY
                       "&output=JSON&numResults=1&fields=storyDate,audio"))

(defn- ^:private get-xml-node [parent-node tag]
  (some->> parent-node
           :content
           (filter #(= (:tag %) tag))
           first))

(defn- ^:private get-xml-node-content [parent-node tag]
  (some-> (get-xml-node parent-node tag) :content first))

(defn- ^:private get-xml-node-attribute [parent-node tag attr]
  (some-> (get-xml-node parent-node tag) :attrs attr))

(defn- ^:private xml-parser [text]
  (let [parsed-xml (try
                     (xml-seq (xml/parse-str text))
                     (catch Exception e
                       (do
                         (print (str "Exception: " (.getMessage e) "Unable to parse: " text))
                         nil)))
        showTitle (->> parsed-xml
                       (filter #(= (:tag %) :title))
                       first :content first)
        genre (some->> parsed-xml
                       (filter #(= (:tag %) (name/canonical-name "http://www.itunes.com/dtds/podcast-1.0.dtd" "category" "itunes")))
                       first :attrs first val)
        showUrl (some->> parsed-xml
                         (filter #(= (:tag %) :link))
                         first :content first)
        first-item (some->> parsed-xml
                            (filter
                              (fn [x] (->> x
                                           :content
                                           (some #(= (:tag %) :enclosure)))))
                            first)
        episodeTitle (get-xml-node-content first-item :title)
        pubDate (get-xml-node-content first-item :pubDate)
        url (get-xml-node-attribute first-item :enclosure :url)]
    {:url url :pubDate pubDate :episodeTitle episodeTitle :showTitle showTitle :showUrl showUrl :genre genre}))

;; currently only implemented for npr specific endpoints
(defn- ^:private api-parser [response]
  (let [json-response (json/read-str response)
        latest-story (get-in json-response ["list" "story" 0])
        story-url (get-in latest-story ["audio" 0 "format" "mp3" 0 "$text"])
        story-date (get-in latest-story ["storyDate" "$text"])]
    {:url story-url :pubDate story-date}))

(defrecord Resource [name url parser post-processing-fn])

(defn xml-resource
  ([name url] (->Resource name url xml-parser identity))
  ([name url post-processing-fn] (->Resource name url xml-parser post-processing-fn)))

(defn api-resource [name url]
  (->Resource name url api-parser identity))

(defn override-title [title]
  #(assoc % :showTitle title))

(defn update-attribute [attr func]
  #(update-in % [attr] func))

(def ^:private NEWSCAST-ENDPOINTS
  [(api-resource :npr NPR-ENDPOINT)
   (xml-resource :pri "http://www.pri.org/programs/3704/episodes/feed")
   (xml-resource :bbc-global "http://www.bbc.co.uk/programmes/p02nq0gn/episodes/downloads.rss")
   (xml-resource :marketplace "http://feeds.publicradio.org/public_feeds/marketplace-pm/rss/rss")
   (xml-resource :democracynow "http://www.democracynow.org/podcast.xml")
   (xml-resource :pbs "http://feeds.feedburner.com/NewshourFullProgramPodcast?format=xml")
   (xml-resource :bbc-africa "http://www.bbc.co.uk/programmes/p02nrtyw/episodes/downloads.rss")])

(def ^:private PODCAST-ENDPOINTS
  [(xml-resource :reveal "http://feeds.revealradio.org/revealpodcast.xml")
   (xml-resource :nypl "http://newyorkpubliclibrary.libsyn.com/rss" (override-title "The NYPL Podcast"))
   (xml-resource :in-our-time "http://www.bbc.co.uk/programmes/b006qykl/episodes/downloads.rss")
   (xml-resource :open-source "http://radioopensource.org/feed/" (override-title "Open Source"))
   (xml-resource :radiolab "http://feeds.wnyc.org/radiolab" (update-attribute :episodeTitle str/trim))
   (xml-resource :factmag "http://factmag.squarespace.com/factmixes?format=RSS" (override-title "FACT Mixes"))
   (xml-resource :homebrave "http://feeds.feedburner.com/homebravepodcast")
   (xml-resource :rumble "http://www.rumblestripvermont.com/feed/")
   (xml-resource :guardian "https://www.theguardian.com/news/series/the-audio-long-read/podcast.xml"
                 (comp
                   (override-title "Long Reads")
                   (update-attribute :episodeTitle str/trim)
                   (update-attribute :episodeTitle #(str/replace % " – podcast" ""))))
   (xml-resource :unfictional "http://feeds.kcrw.com/kcrw/uf" (override-title "UnFictional"))
   (xml-resource :organist "http://feeds.kcrw.com/kcrw/to" (override-title "The Organist"))
   (xml-resource :shortcuts "http://www.bbc.co.uk/programmes/b01mk3f8/episodes/downloads.rss")
   (xml-resource :seriously "http://www.bbc.co.uk/programmes/p02pc9qx/episodes/downloads.rss")
   (xml-resource :bodegaboys "http://feeds.soundcloud.com/users/soundcloud:users:169774121/sounds.rss")
   (xml-resource :snapjudgement "http://feeds.wnyc.org/snapjudgment-wnyc")
   (xml-resource :worldinwords "http://feeds.feedburner.com/pri/world-words")
   (xml-resource :chapos-traphouse "http://feeds.soundcloud.com/users/soundcloud:users:211911700/sounds.rss"
                 (update-attribute :episodeTitle #(str/replace % #"\s\(\d{1}/\d{2}/\d{2}\)" "")))
   (xml-resource :desert-island-discs "http://www.bbc.co.uk/programmes/b006qnmr/episodes/downloads.rss")
   ])

(defn ^:private get-ajax-channel [{:keys [url name parser post-processing-fn]}]
  (let [c (chan)]
    (httpkit/get url #(go (>! c
                              {name (some-> (:body %)
                                            parser
                                            (assoc :rssUrl url)
                                            post-processing-fn)})))
    c))

(defn ^:private get-resources [endpoints]
  (let [channels (map get-ajax-channel endpoints)
        results (atom {})]
    (go (doseq [chan channels]
          (swap! results conj (<! chan))))
    (loop [res @results]
      (if (= (count res) (count endpoints))
        res
        (recur @results)))))

(defn get-newscasts []
  (get-resources NEWSCAST-ENDPOINTS))

(defn get-podcasts []
  (get-resources PODCAST-ENDPOINTS))
