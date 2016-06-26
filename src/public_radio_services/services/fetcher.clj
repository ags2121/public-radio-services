(ns public-radio-services.services.fetcher
  (:require [org.httpkit.client :as httpkit]
            [clojure.core.async :refer [chan go >! <!]]
            [clojure.data.json :as json]
            [environ.core :refer [env]]
            [clojure.data.xml :as xml]))

(def NPR-API-KEY (env :npr-api-key))
(def NPR-ENDPOINT (str "https://api.npr.org/query?id=500005&profileTypeId=15"
                       "&apiKey=" NPR-API-KEY
                       "&output=JSON&numResults=1&fields=storyDate,audio"))

(defn- ^:private get-xml-node [parent-node tag]
  (->> parent-node
       :content
       (filter #(= (:tag %) tag))
       first))

(defn- ^:private get-xml-node-content [parent-node tag]
  (-> (get-xml-node parent-node tag) :content first))

(defn- ^:private get-xml-node-attribute [parent-node tag attr]
  (-> (get-xml-node parent-node tag) :attrs attr))

(defn- ^:private rss-parser [text]
  (let [first-item (->> (xml/parse-str text)
                        xml-seq
                        (filter #(= (:tag %) :item))
                        first)
        title (get-xml-node-content first-item :title)
        pubDate (get-xml-node-content first-item :pubDate)
        url (get-xml-node-attribute first-item :enclosure :url)]

    {:url url :pubDate pubDate :title title}))

(defn- ^:private npr-parser [response]
  (let [json-response (json/read-str response)
        latest-story (get-in json-response ["list" "story" 0])
        story-url (get-in latest-story ["audio" 0 "format" "mp3" 0 "$text"])
        story-date (get-in latest-story ["storyDate" "$text"])]
    {:url story-url :pubDate story-date}))

(defrecord Resource [type url parser])

(defn resource
  ([type url parser] (->Resource type url parser))
  ([type url] (->Resource type url rss-parser)))

(def ^:private NEWSCAST-ENDPOINTS
  [(resource :npr NPR-ENDPOINT npr-parser)
   (resource :pri "http://www.pri.org/programs/3704/episodes/feed")
   (resource :bbc-global "http://www.bbc.co.uk/programmes/p02nq0gn/episodes/downloads.rss")
   (resource :marketplace "http://feeds.publicradio.org/public_feeds/marketplace-pm/rss/rss")
   (resource :democracynow "http://www.democracynow.org/podcast.xml")
   (resource :pbs "http://feeds.feedburner.com/NewshourFullProgramPodcast?format=xml")
   (resource :bbc-africa "http://www.bbc.co.uk/programmes/p02nrtyw/episodes/downloads.rss")])

(def ^:private PODCAST-ENDPOINTS
  [(resource :reveal "http://feeds.revealradio.org/revealpodcast.xml")
   (resource :nypl "http://newyorkpubliclibrary.libsyn.com/rss")
   (resource :in-our-time "http://www.bbc.co.uk/programmes/b006qykl/episodes/downloads.rss")
   (resource :open-source "http://radioopensource.org/feed/")
   (resource :radiolab "http://feeds.wnyc.org/radiolab")
   (resource :radiotonic "http://www.abc.net.au/radionational/feed/5421356/podcast.xml")
   (resource :factmag "http://factmag.squarespace.com/factmixes?format=RSS")
   (resource :homebrave "http://feeds.feedburner.com/homebravepodcast")
   (resource :rumble "http://www.rumblestripvermont.com/feed/")
   (resource :ideas "http://www.cbc.ca/podcasting/includes/ideas.xml")
   (resource :unfictional "http://feeds.kcrw.com/kcrw/uf")
   (resource :organist "http://feeds.kcrw.com/kcrw/to")
   (resource :shortcuts "http://www.bbc.co.uk/programmes/b01mk3f8/episodes/downloads.rss")
   (resource :seriously "http://www.bbc.co.uk/programmes/p02pc9qx/episodes/downloads.rss")])

(defn ^:private get-ajax-channel [news-source]
  (let [c (chan)
        url (:url news-source)
        type (:type news-source)
        parser (:parser news-source)]
    (httpkit/get url #(go (>! c {type (parser (:body %))})))
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
