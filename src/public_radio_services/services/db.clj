(ns public-radio-services.services.db
  (:require
    [clojure.java.jdbc :as jdbc]
    [environ.core :refer [env]]))

(def spec (env :database-url))

(defn ^:private migrated? []
  (-> (jdbc/query spec
                  [(str "select count(*) from information_schema.tables "
                        "where table_name='request'")])
      first :count pos?))

(defn migrate []
  (when (not (migrated?))
    (print "Creating database structure...") (flush)
    (jdbc/db-do-commands spec
                         (jdbc/create-table-ddl
                           :request
                           [:id :serial "PRIMARY KEY"]
                           [:name "VARCHAR(200)" "NOT NULL"]
                           [:requestor "VARCHAR(100)"]
                           [:email "VARCHAR(30)"]
                           [:url "VARCHAR(2083)"]
                           [:created_at :timestamp
                            "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]))
    (println " done")))

(defn get-requests []
  (jdbc/query spec ["select * from request order by id desc"]))

(defn save-request! [request]
  (first (jdbc/insert! spec :request request)))