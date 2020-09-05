(ns public-radio-services.services.fetcher
  (:require [clojure.core.async :refer [chan go >! <!]]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [clojure.data.xml :as xml]
            [clojure.data.xml.name :as name]
            [clojure.string :as str]
            [clj-http.client :as client]
            [net.cgrand.enlive-html :as html]
            [clojure.java.io :as io]))

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
        showTitle (some->> parsed-xml
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

(defn- ^:private bbc-news-summary-parser [body]
  (let [data (json/read-str (second (re-find #"window.INITIAL_STATE=(.+);"
                                     (-> (html/html-resource (io/input-stream (.getBytes body)))
                                         (html/select [:head :script])
                                         second
                                         :content
                                         first))))
        data-id (-> data
                    (get "profiles")
                    first
                    val
                    (get "actions")
                    (get "play")
                    (get "guideId"))
        url (-> (client/get (str "https://opml.radiotime.com/Tune.ashx?&id=" data-id "&render=json&formats=mp3,aac,ogg,flash,html&version=2&itemUrlScheme=secure") {:as :json})
                :body
                :body
                first
                :url)]
    {:url url}))

(defrecord Resource [name type url parser post-processing-fn])

(defn xml-resource
  ([name type url] (->Resource name type url xml-parser identity))
  ([name type url post-processing-fn] (->Resource name type url xml-parser post-processing-fn)))

(defn api-resource [name url]
  (->Resource name :newscast url api-parser identity))

(defn bbc-news-summary-resource [name url]
  (->Resource name :newscast url bbc-news-summary-parser identity))

(defn override-title [title]
  #(assoc % :showTitle title))

(defn update-attribute [attr func]
  #(update-in % [attr] func))

(def NEWSCAST-ENDPOINTS
  [
   ;(api-resource :npr NPR-ENDPOINT)
   (xml-resource :npr :newscast "https://www.npr.org/rss/podcast.php?id=500005")
   (bbc-news-summary-resource :bbc-headlines "https://tunein.com/radio/BBC-News-Summary-p193595/")
   (xml-resource :pri :newscast "http://www.pri.org/programs/3704/episodes/feed")
   (xml-resource :bbc-global :newscast "http://www.bbc.co.uk/programmes/p02nq0gn/episodes/downloads.rss")
   (xml-resource :democracynow :newscast "http://www.democracynow.org/podcast.xml")
   (xml-resource :bbc-africa :newscast "http://www.bbc.co.uk/programmes/p02nrtyw/episodes/downloads.rss")])


(def PODCAST-ENDPOINTS
  [
   (xml-resource :london-review :podcast "https://cdn.lrb.co.uk/feeds/podcasts" (override-title "The London Review"))
   (xml-resource :rumble :podcast "https://www.rumblestripvermont.com/feed/")
   (xml-resource :reveal :podcast "http://feeds.revealradio.org/revealpodcast.xml")
   ; (xml-resource :nypl :podcast "http://newyorkpubliclibrary.libsyn.com/rss" (override-title "The NYPL Podcast")) ; broken
   (xml-resource :in-our-time :podcast "http://www.bbc.co.uk/programmes/b006qykl/episodes/downloads.rss")
   (xml-resource :open-source :podcast "http://radioopensource.org/feed/" (override-title "Open Source"))
   (xml-resource :homebrave :podcast "http://feeds.feedburner.com/homebravepodcast")
   (xml-resource :guardian :podcast "https://www.theguardian.com/news/series/the-audio-long-read/podcast.xml"
                 (comp
                   (override-title "Long Reads")
                   (update-attribute :episodeTitle str/trim)
                   (update-attribute :episodeTitle #(str/replace % " – podcast" ""))))
   (xml-resource :unfictional :podcast "https://www.kcrw.com/news-culture/shows/unfictional/rss.xml" (override-title "UnFictional"))
   (xml-resource :organist :podcast "https://www.kcrw.com/news-culture/shows/the-organist/rss.xml" (override-title "The Organist"))
   (xml-resource :shortcuts :podcast "http://www.bbc.co.uk/programmes/b01mk3f8/episodes/downloads.rss")
   (xml-resource :seriously :podcast "http://www.bbc.co.uk/programmes/p02pc9qx/episodes/downloads.rss")
   (xml-resource :bodegaboys :podcast "http://feeds.soundcloud.com/users/soundcloud:users:169774121/sounds.rss")
   (xml-resource :mouth-time :podcast "http://feeds.feedburner.com/MouthTimeWithReductress" (override-title "Mouth Time"))
   (xml-resource :snapjudgement :podcast "http://feeds.wnyc.org/snapjudgment-wnyc")
   (xml-resource :worldinwords :podcast "http://feeds.feedburner.com/pri/world-words")
   (xml-resource :chapos-traphouse :podcast "http://feeds.soundcloud.com/users/soundcloud:users:211911700/sounds.rss"
                 (update-attribute :episodeTitle #(str/replace % #"\s\(\d{1}/\d{2}/\d{2}\)" "")))
   (xml-resource :desert-island-discs :podcast "http://www.bbc.co.uk/programmes/b006qnmr/episodes/downloads.rss")
   (xml-resource :resident-advisor :podcast "https://www.residentadvisor.net/xml/podcast.xml" (override-title "Resident Advisor"))
   (xml-resource :eternal-now :podcast "https://wfmu.org/podcast/AO.xml" (override-title "The Eternal Now"))
   (xml-resource :honky-tonk :podcast "https://wfmu.org/podcast/HG.xml" (override-title "Honky Tonk Radio Girl"))
   (xml-resource :intercept :podcast "http://feeds.megaphone.fm/intercepted" (override-title "Intercepted"))
   (xml-resource :between-the-ears :podcast "http://www.bbc.co.uk/programmes/b006x2tq/episodes/downloads.rss")
   (xml-resource :the-essay :podcast "http://www.bbc.co.uk/programmes/b006x3hl/episodes/downloads.rss")
   (xml-resource :fishko-files :podcast "http://feeds.wnyc.org/fishko" (override-title "Fishko Files"))
   (xml-resource :bird-note :podcast "http://feeds.feedburner.com/birdnote/OYfP")
   (xml-resource :dead-pundits :podcast "http://feeds.soundcloud.com/users/soundcloud:users:292981343/sounds.rss")
   (xml-resource :upstream :podcast "http://feeds.soundcloud.com/users/soundcloud:users:200783566/sounds.rss")
   (xml-resource :bafflercasts :podcast "https://thebaffler.com/feed/podcast")
   (xml-resource :moderate-rebels :podcast "https://moderaterebels.libsyn.com/rss")
   (xml-resource :delete-your-acct :podcast "https://deleteyouraccount.libsyn.com/showrss")
   (xml-resource :citations :podcast "https://citationsneeded.libsyn.com/rss")
   (xml-resource :by-any-means-necessary :podcast "https://www.spreaker.com/show/1843722/episodes/feed")
   (xml-resource :kt-halps :podcast "https://feeds.soundcloud.com/users/soundcloud:users:54379684/sounds.rss")
   (xml-resource :discoursecollective :podcast "https://feeds.soundcloud.com/users/soundcloud:users:287122696/sounds.rss")
   (xml-resource :art-and-labor :podcast "http://artandlaborpodcast.com/feed/podcast")
   (xml-resource :trillbilly :podcast "http://feeds.soundcloud.com/users/soundcloud:users:300222802/sounds.rss")
   (xml-resource :street-fight :podcast "http://feeds.feedburner.com/streetfightradio?format=xml")])



(def ENDPOINTS (into [] (concat NEWSCAST-ENDPOINTS PODCAST-ENDPOINTS)))

(defn ^:private get-ajax-channel [{:keys [url type name parser post-processing-fn]}]
  (let [c (chan)]
    (client/get
      url
      {:async? true}
      #(go (>! c
               {name (some-> (:body %)
                             parser
                             (assoc :sourceUrl url :type type)
                             post-processing-fn)}))
      (fn [exception] (println "exception message is: " (.getMessage exception))))
    c))

(defn get-resources [endpoints]
  (let [channels (map get-ajax-channel endpoints)
        results (atom {})]
    (go (doseq [chan channels]
          (swap! results conj (<! chan))))
    (loop [res @results]
      (if (= (count res) (count endpoints))
        res
        (recur @results)))))

(def cache (atom {}))

(def newscasts-position (atom 0))

(def podcasts-position (atom 0))

(def all-position (atom 0))

(defn add-to-cache [position step endpoints]
  (let [pos @position
        indices (take step (drop pos (cycle (range 0 (dec (count endpoints))))))
        endoints-to-fetch (mapv endpoints indices)
        data (get-resources endoints-to-fetch)]
    (do
      (swap! cache merge data)
      (swap! position + step)
      (println "cache refreshed with: " (keys data)))))

(defn add-all-to-cache []
  (let [step 10]
    (dotimes [_ (Math/ceil (/ (count ENDPOINTS) step))] (add-to-cache all-position step ENDPOINTS))))

(defn add-newscasts-to-cache []
  (add-to-cache newscasts-position 5 NEWSCAST-ENDPOINTS))

(defn add-podcasts-to-cache []
  (add-to-cache podcasts-position 2 PODCAST-ENDPOINTS))

(defn get-data-of-type [type]
  (select-keys @cache (for [[k v] @cache :when (= type (-> v :type))] k)))

(defn get-newscasts []
  (get-resources NEWSCAST-ENDPOINTS))

(defn get-podcasts []
  (get-resources PODCAST-ENDPOINTS))
