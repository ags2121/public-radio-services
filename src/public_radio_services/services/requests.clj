(ns public-radio-services.services.requests
  (:require
    ;[datomic.api :only [db q] :as d]
    [public-radio-services.services.postgres :as p]
    [environ.core :refer [env]]
    [clojure.string :only [blank?] :as string]
    [org.httpkit.client :only [head] :as httpkit])
  (:import (java.util Date TimeZone)))

(defn ^:private is-valid-url [url]
  (let [url (if (nil? (re-matches #"^(https?)://.*$" url))
              (str "http://" url)
              url)]
    (nil? (:error @(httpkit/head url)))))

(defn validate-request [{info "info" url "url"}]
  (let [errors {}
        errors (conj errors (if (string/blank? info)
                              {::info ::not-present}
                              (if (> (count info) 200)
                                {::info ::too-long}
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
  (p/get-requests))

(defn save-request! [request]
  (p/save-request! request))
