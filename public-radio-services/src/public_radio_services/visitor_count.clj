(ns public-radio-services.visitor-count
  (:require [clj-time.core :as t])
  (:import [java.util UUID]))

(def COOKIE-NAME "public-radio")

(def ^:private stored-cookies (atom {}))

(defn ^:private save-or-update-cookie! [cookie time]
  (swap! stored-cookies assoc cookie time))

(defn- ^:private calculate-visitor-count [cookies current-time]
  (let [five-mins-ago (t/minus current-time (t/minutes 5))]
    (count (filter #(t/within? five-mins-ago current-time (val %1)) cookies))))

(defn get-visitor-count [request-cookies]
  (let [cookie (or
                 (:value (get request-cookies COOKIE-NAME))
                 (.toString (UUID/randomUUID)))
        now (t/now)
        updated-cookies (save-or-update-cookie! cookie now)
        visitor-count (calculate-visitor-count updated-cookies now)]
    {:body    {:count visitor-count}
     :cookies {COOKIE-NAME cookie}}))
