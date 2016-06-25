(ns public-radio-services.services.datomic
  (:require
    ;[datomic.api :only [db q] :as d]
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

(defn get-requests []
  (->> (d/q '[:find (pull ?e [*]) ?ts
              :in $ %
              :where
              (attr-in-namespace ?a "request")
              [?e ?a ?v ?tx]
              [?tx :db/txInstant ?ts]]
            (db) rules)
       (map #(conj (first %) {:request/time (second %)}))
       (sort-by :db/id >)))

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
