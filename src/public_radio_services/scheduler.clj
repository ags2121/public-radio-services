(ns public-radio-services.scheduler
  (:require [overtone.at-at :refer [every mk-pool]]
            [clj-http.client :as client]
            [public-radio-services.services.fetcher :as f]
            [tick.core :refer [seconds]]
            [tick.timeline :refer [timeline periodic-seq]]
            [tick.clock :refer [now]]
            [tick.schedule]))

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

;; every 30 seconds add the pods and news to cache
;(defn pre-cache-scheduler []
;  (interspaced 60000 f/add-to-cache my-pool))

(defn pre-cache-scheduler-2 []
  (let [schedule (tick.schedule/schedule f/add-to-cache (timeline (periodic-seq (now) (seconds 60))))]
    (tick.schedule/start schedule (tick.clock/clock-ticking-in-seconds))))
