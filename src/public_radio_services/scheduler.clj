(ns public-radio-services.scheduler
  (:require [overtone.at-at :refer :all]
            [clj-http.client :as client]
            [public-radio-services.services.fetcher :as f]))

(def my-pool (mk-pool))

(defn get-requests []
  (client/get
    "http://www.publicradioservices.info/visitor-count"
    {:async? true}
    (fn [_] (println "server polled"))
    (fn [exception] (println "exception message is: " (.getMessage exception)))))

;; every half hour poll the server
(defn poll-server []
  (every 1800000 get-requests my-pool))

;; every 60 seconds add a slice of news to cache
(defn pre-cache-news-scheduler []
  (interspaced 60000 f/add-newscasts-to-cache my-pool))

;; every 5 1/2 minutes add a slice of pods to cache
(defn pre-cache-pod-scheduler []
  (interspaced 330000 f/add-podcasts-to-cache my-pool))
