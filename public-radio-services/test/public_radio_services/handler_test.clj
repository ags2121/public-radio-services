(ns public-radio-services.handler-test
  (:require [clojure.test :refer [deftest testing is]]
            [ring.mock.request :refer [request header]]
            [public-radio-services.handler :as h]
            [public-radio-services.visitor-count :as vc]))

(defn- get-cookie [response]
  (get (:cookies response) vc/COOKIE-NAME))

(defn- add-cookie [request cookie]
  (assoc request :cookies {vc/COOKIE-NAME {:value cookie}}))

(deftest visitor-count-with-no-cookie
  (testing "request has no cookie, add cookie to response"
    (let [response (h/routes (request :get "/visitor-count"))]
      (is (= (:status response 200)))
      (is (not (empty (get-cookie response)))))))

(deftest visitor-count-with-cookie
  (testing "request has cookie, same cookie is in response"
    (let [cookie "1234"
          response (h/routes (add-cookie (request :get "/visitor-count") cookie))]
      (is (= (:status response 200)))
      (is (=
            (get-cookie response)
            cookie)))))

(deftest news
  (testing "news endpoint"
    (let [response (h/routes (request :get "/news"))]
      (is (= (:status response 200))))))
