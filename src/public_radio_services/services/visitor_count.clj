(ns public-radio-services.services.visitor-count
  (:require [clj-time.core :as t])
  (:import [java.util UUID]))

(def COOKIE-NAME "public-radio")

(def ^:private stored-cookies (atom {}))

(defn ^:private save-or-update-cookie! [cookie time]
  (swap! stored-cookies assoc cookie time))

(defn- ^:private calculate-visitor-count [cookies current-time]
  (let [ago (t/minus current-time (t/hours 24))]
    (count (filter #(t/within? ago current-time (val %1)) cookies))))

(defn ^:private delete-cookie! [cookie]
  (swap! stored-cookies dissoc cookie))

(defn get-visitor-count [request-cookies]
  (let [cookie (or
                 (:value (get request-cookies COOKIE-NAME))
                 (.toString (UUID/randomUUID)))
        now (t/now)
        updated-cookies (save-or-update-cookie! cookie now)
        visitor-count (calculate-visitor-count updated-cookies now)]
    {:count visitor-count
     :cookie cookie}))

(defn delete-visitor! [request-cookies]
  (if-let [cookie (:value (get request-cookies COOKIE-NAME))]
    (delete-cookie! cookie)))
