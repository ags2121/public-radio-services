(ns public-radio-services.handler
  (:require [compojure.core :refer [GET defroutes]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params         :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [public-radio-services.visitor-count :as vc]))

(defn wrap-log-request [handler]
  (fn [req]
    (println req)
    (handler req)))

(defroutes routes
  (GET "/visitor-count" {cookies :cookies}
    (vc/get-visitor-count cookies))
  (GET "/news" [] {:body {:news {}}}))

(def app (-> routes
             wrap-log-request
             wrap-cookies
             wrap-params
             wrap-json-response))
