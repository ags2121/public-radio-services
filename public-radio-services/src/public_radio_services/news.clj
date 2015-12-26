(ns public-radio-services.news
  (:require [org.httpkit.client :as httpkit]
            [clojure.edn :as edn]
            [clojure.core.async :refer [chan go alts! >! <!]]))

(def news-cache (atom {}))

(def NPR-API-KEY
  (:npr-api-key (edn/read-string (slurp "src/public_radio_services/config.edn"))))

(def NEWS-ENDPOINTS
  [{:type "pri"
    :url "http://www.pri.org/programs/3704/episodes/feed"}
   {:type "bbc-global"
    :url "http://www.bbc.co.uk/programmes/p02nq0gn/episodes/downloads.rss"}
   {:type "npr"
    :url  (str "https://api.npr.org/query?id=500005"
               "profileTypeId=15&meta=inherit&apiKey="
               NPR-API-KEY
               "&output=JSON&numResults=1&fields=storyDate,audio")}])

(defn get-ajax-channel [news-source]
  (let [c (chan)]
    (httpkit/get (:url news-source) #(go (>! c {(:type news-source) %})))
    c))

(defn get-news [news-sources]
  (let [channels (map get-ajax-channel news-sources)
        results (atom [])]
    (go (doseq [chan channels]
          (swap! results conj (<! chan))))
    (loop [res @results]
      (if (= (count res) (count news-sources))
        res
        (recur @results)))))

;var podcastInfos = [{
;                     type: 'pri',
;                     url: 'http://www.pri.org/programs/3704/episodes/feed',
;parseFunction: rssParseFunction,
;protocol: http
;}, {
;    type: 'bbc-global',
;    url: 'http://www.bbc.co.uk/programmes/p02nq0gn/episodes/downloads.rss',
;parseFunction: rssParseFunction,
;protocol: http
;}, {
;    type: 'npr',
;    url: 'https://api.npr.org/query?id=500005&profileTypeId=15&meta=inherit&apiKey=' + nprKey + '&output=JSON&numResults=1&fields=storyDate,audio',
;parseFunction: nprApiParseFunction,
;protocol: https
;}];
;
;(def get-news []
;
;  )
