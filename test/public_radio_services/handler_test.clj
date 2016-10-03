(ns public-radio-services.handler-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [ring.mock.request :refer [request header]]
            [public-radio-services.handler :as h]
            [public-radio-services.services.visitor-count :as vc]
            [datomic.api :only [db q] :as d]
            [environ.core :refer [env]]))

(defn create-db [f]
  (d/create-database (env :database-url))
  (f))

(use-fixtures :once create-db)

(defn- get-cookie [response]
  (get (:cookies response) vc/COOKIE-NAME))

(defn- add-cookie [request cookie]
  (assoc request :cookies {vc/COOKIE-NAME {:value cookie}}))

(deftest visitor-count-with-no-cookie
  (testing "request has no cookie, add cookie to response"
    (let [response (h/public-routes (request :get "/visitor-count"))]
      (is (= (:status response 200)))
      (is (not (empty (get-cookie response)))))))

(deftest visitor-count-with-cookie
  (testing "request has cookie, same cookie is in response"
    (let [cookie "1234"
          response (h/public-routes (add-cookie (request :get "/visitor-count") cookie))]
      (is (= (:status response 200)))
      (is (=
            (get-cookie response)
            cookie)))))

(deftest news
  (testing "news endpoint"
    (let [response (h/public-routes (request :get "/news"))]
      (is (= (:status response 200))))))
