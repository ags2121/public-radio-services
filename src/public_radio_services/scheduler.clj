(ns public-radio-services.scheduler
  (:require [overtone.at-at :refer :all]
            [org.httpkit.client :as httpkit]
            [public-radio-services.services.fetcher :as f]))

(def my-pool (mk-pool))

(defn get-requests []
  (httpkit/get "http://www.publicradioservices.info/visitor-count" (fn [_] (println "server polled"))))

;; every half hour poll the server
(defn poll-server []
  (every 1800000 get-requests my-pool))

;; every 30 seconds add the pods and news to cache
(defn pre-cache-scheduler []
  (every 30000 f/add-to-cache my-pool))
