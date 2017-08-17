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

;; every 60 seconds add a slice of the pods and news to cache
(defn pre-cache-scheduler []
  (interspaced 60000 f/add-to-cache my-pool))
