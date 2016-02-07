(ns public-radio-services.services.requests
  (:require [mount.core :refer [defstate]]
            [datomic.api :only [db q] :as d]
            [environ.core :refer [env]]
            [clojure.string :only [blank?] :as string]
            [org.httpkit.client :only [head] :as httpkit]))

; these are functions and not values because I couldn't compile the tests with them as values
(defn conn [] (d/connect (env :database-url)))
(defn db [] (d/db (conn)))

(def rules
  '[[[attr-in-namespace ?e ?ns2]
     [?e :db/ident ?a]
     [?e :db/valueType]
     [(namespace ?a) ?ns1]
     [(= ?ns1 ?ns2)]]])

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

(defn get-requests []
  (map first (d/q '[:find (pull ?e [*])
                    :in $ %
                    :where
                    (attr-in-namespace ?a "request")
                    [?e ?a]]
                  (db) rules)))

(defn save-request! [{info "info" url "url" requestor "requestor"}]
  (if (not (string/blank? info))
    (let [tx-data {:db/id        #db/id[:db.part/user]
                   :request/info info}
          tx-data-with-url (conj tx-data (if (string/blank? url)
                                           {}
                                           {:request/url url}))
          tx-data-with-requestor (conj tx-data-with-url (if (string/blank? requestor)
                                                          {}
                                                          {:request/requestor requestor}))]
      @(d/transact (conn) [tx-data-with-requestor]))))
