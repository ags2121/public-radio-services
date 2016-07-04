(ns public-radio-services.services.requests
  (:require [public-radio-services.services.db :as db]
            [environ.core :refer [env]]
            [clojure.string :only [blank?] :as string]
            [org.httpkit.client :only [head] :as httpkit])
  (:import (java.util Date TimeZone)))

(defn ^:private is-valid-url [url]
  (let [url (if (nil? (re-matches #"^(https?)://.*$" url))
              (str "http://" url)
              url)]
    (nil? (:error @(httpkit/head url)))))

(defn validate-request [{name "name" url "url"}]
  (let [errors {}
        errors (conj errors (if (string/blank? name)
                              {::name ::not-present}
                              (if (> (count name) 200)
                                {::name ::too-long}
                                {})))
        errors (conj errors (if (and
                                  (not (string/blank? url))
                                  (not (is-valid-url url)))
                              {::url ::not-valid}
                              {}))]
    errors))

(defn ^:private format-date [^Date date]
  (.format (doto
             (java.text.SimpleDateFormat. "MMM dd, yyyy hh:mm:ss a z")
             (.setTimeZone (TimeZone/getTimeZone "EST")))
           date))

(defn get-requests []
  (db/get-requests))

(defn save-request! [request]
  (db/save-request! request))
