(ns public-radio-services.services.db
  (:require
    [clojure.java.jdbc :as jdbc]
    [environ.core :refer [env]]
    [buddy.hashers :as hashers]))

(def spec (env :database-url))

(defn ^:private migrated? []
  (-> (jdbc/query spec
                  [(str "select count(*) from information_schema.tables "
                        "where table_name in ('request', 'users')")])
      first :count pos?))

(defn migrate []
  (when (not (migrated?))
    (jdbc/db-do-commands spec
                         ; TODO: Check if created yet
                         (jdbc/create-table-ddl
                           :request
                           [:id :serial "PRIMARY KEY"]
                           [:name "VARCHAR(200)" "NOT NULL"]
                           [:requestor "VARCHAR(100)"]
                           [:email "VARCHAR(30)"]
                           [:url "VARCHAR(2083)"]
                           [:created_at :timestamp
                            "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"])

                         ; TODO: Check if created yet
                         (jdbc/create-table-ddl
                           :users
                           [:id :serial "PRIMARY KEY"]
                           [:username "VARCHAR(200)" "NOT NULL"]
                           [:password "VARCHAR(200)" "NOT NULL"]))))

(defn get-requests []
  (jdbc/query spec ["select * from request order by id desc"]))

(defn save-request! [request]
  (first (jdbc/insert! spec :request request)))

(defn is-admin? [password]
  (hashers/check
    password
    (-> (jdbc/query spec ["select password from users where username = 'admin'"])
        first
        :password)))
