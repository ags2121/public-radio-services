(ns public-radio-services.scheduler
  (:require [overtone.at-at :refer :all]
            [org.httpkit.client :as httpkit]))

(def my-pool (mk-pool))

(defn get-requests []
  (httpkit/get "http://www.publicradioservices.info/visitor-count" (fn [_] (println "server polled"))))

;; every half hour poll the server
(every 1800000 get-requests my-pool)
