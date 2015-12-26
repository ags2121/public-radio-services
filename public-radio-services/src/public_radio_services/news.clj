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

(defn get-feed [news channel]
  (httpkit/get (:url news) #(go (>! channel {(:type news) %}))))

;(defn get-news [news-endpoints]
;  (let [news-count (count news-endpoints)
;        chans (repeatedly news-count chan)
;        results (atom [])]
;    (go (while (not= (count results) news-count)
;          (let [[v ch] (alts! [chans])]
;            (swap! results conj v))))
;    (dotimes [ch chans]
;           (httpkit/get ))
;    )
;  )

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
