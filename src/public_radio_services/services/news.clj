(ns public-radio-services.services.news
  (:require [org.httpkit.client :as httpkit]
            [clojure.core.async :refer [chan go >! <!]]
            [clojure.data.json :as json]
            [environ.core :refer [env]]))

(def NPR-API-KEY (env :npr-api-key))

(defn- ^:private rss-parser [text]
  (let [regex-func (fn [regex] ((re-find regex text) 1))
        story-url (regex-func #"enclosure url=\"(.*?)\"")
        story-date (regex-func #"<pubDate>(.*?)<\/pubDate>")]
    {:url story-url :pubDate story-date}))

(defn- ^:private npr-parser [response]
  (let [json-response (json/read-str response)
        latest-story (get-in json-response ["list" "story" 0])
        story-url (get-in latest-story ["audio" 0 "format" "mp3" 0 "$text"])
        story-date (get-in latest-story ["storyDate" "$text"])]
    {:url story-url :pubDate story-date}))

(def ^:private NEWS-ENDPOINTS
  [{:type :pri
    :url "http://www.pri.org/programs/3704/episodes/feed"
    :parser rss-parser}
   {:type :bbc-global
    :url "http://www.bbc.co.uk/programmes/p02nq0gn/episodes/downloads.rss"
    :parser rss-parser}
   {:type :npr
    :url (str "https://api.npr.org/query?id=500005&profileTypeId=15"
              "&apiKey=" NPR-API-KEY
              "&output=JSON&numResults=1&fields=storyDate,audio")
    :parser npr-parser}])

(defn ^:private get-ajax-channel [news-source]
  (let [c (chan)
        url (:url news-source)
        type (:type news-source)
        parser (:parser news-source)]
    (httpkit/get url #(go (>! c {type (parser (:body %))})))
    c))

(defn get-news []
  (let [channels (map get-ajax-channel NEWS-ENDPOINTS)
        results (atom {})]
    (go (doseq [chan channels]
          (swap! results conj (<! chan))))
    (loop [res @results]
      (if (= (count res) (count NEWS-ENDPOINTS))
        res
        (recur @results)))))
