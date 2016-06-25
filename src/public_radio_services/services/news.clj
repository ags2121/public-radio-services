(ns public-radio-services.services.news
  (:require [org.httpkit.client :as httpkit]
            [clojure.core.async :refer [chan go >! <!]]
            [clojure.data.json :as json]
            [environ.core :refer [env]]))

(def NPR-API-KEY (env :npr-api-key))
(def NPR-ENDPOINT (str "https://api.npr.org/query?id=500005&profileTypeId=15"
                       "&apiKey=" NPR-API-KEY
                       "&output=JSON&numResults=1&fields=storyDate,audio"))

(defn- ^:private rss-parser [text]
  (let [regex-func (fn [regex] ((re-find regex text) 1))
        story-url (regex-func #"enclosure .*url=\"(.*?)\"")
        story-date (regex-func #"<pubDate>(.*?)<\/pubDate>")
        story-title (second (rest (re-seq #"<title>(.*?)<\/title>" text)))]

    {:url story-url :pubDate story-date :title story-title}))

(defn- ^:private npr-parser [response]
  (let [json-response (json/read-str response)
        latest-story (get-in json-response ["list" "story" 0])
        story-url (get-in latest-story ["audio" 0 "format" "mp3" 0 "$text"])
        story-date (get-in latest-story ["storyDate" "$text"])]
    {:url story-url :pubDate story-date}))

(defrecord Resource [type url parser])

(def ^:private NEWSCAST-ENDPOINTS
  [(Resource. :pri "http://www.pri.org/programs/3704/episodes/feed" rss-parser)
   (Resource. :bbc-global "http://www.bbc.co.uk/programmes/p02nq0gn/episodes/downloads.rss" rss-parser)
   (Resource. :npr NPR-ENDPOINT npr-parser)
   (Resource. :marketplace "http://feeds.publicradio.org/public_feeds/marketplace-pm/rss/rss" rss-parser)
   (Resource. :democracynow "http://www.democracynow.org/podcast.xml" rss-parser)
   (Resource. :pbs "http://feeds.feedburner.com/NewshourFullProgramPodcast?format=xml" rss-parser)
   (Resource. :bbc-africa "http://www.bbc.co.uk/programmes/p02nrtyw/episodes/downloads.rss" rss-parser)])

(def ^:private PODCAST-ENDPOINTS
  [
   (Resource. :reveal "http://feeds.revealradio.org/revealpodcast.xml" rss-parser)
   (Resource. :nypl "http://newyorkpubliclibrary.libsyn.com/rss" rss-parser)
   (Resource. :in-our-time "http://www.bbc.co.uk/programmes/b006qykl/episodes/downloads.rss" rss-parser)
   (Resource. :open-source "http://radioopensource.org/feed/" rss-parser)
   (Resource. :radiolab "http://feeds.wnyc.org/radiolab" rss-parser)
   ;(Resource. :radiotonic "http://www.abc.net.au/radionational/feed/5421362/rss.xml" rss-parser)
   (Resource. :factmag "http://factmag.squarespace.com/factmixes?format=RSS" rss-parser)
   (Resource. :homebrave "http://feeds.feedburner.com/homebravepodcast" rss-parser)
   (Resource. :rumble "http://www.rumblestripvermont.com/feed/" rss-parser)
   (Resource. :ideas "http://www.cbc.ca/podcasting/includes/ideas.xml" rss-parser)
   (Resource. :unfictional "http://feeds.kcrw.com/kcrw/uf" rss-parser)
   (Resource. :organist "http://feeds.kcrw.com/kcrw/to" rss-parser)
   (Resource. :shortcuts "http://www.bbc.co.uk/programmes/b01mk3f8/episodes/downloads.rss" rss-parser)
   (Resource. :seriously "http://www.bbc.co.uk/programmes/p02pc9qx/episodes/downloads.rss" rss-parser)
   ])

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
