(ns public-radio-services.handler
  (:require [compojure.core :refer [GET defroutes]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params         :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [public-radio-services.visitor-count :as vc]
            [public-radio-services.news :as news]))

(defn wrap-log-request [handler]
  (fn [req]
    (println req)
    (handler req)))

(defroutes routes
  (GET "/visitor-count" {cookies :cookies}
    (let [{cookie :cookie count :count} (vc/get-visitor-count cookies)]
      {:cookies {vc/COOKIE-NAME cookie}
       :body    {:count count}}))
  (GET "/news" []
    {:body {:news (news/get-news)}}))

(def app (-> routes
             wrap-log-request
             wrap-cookies
             wrap-params
             wrap-json-response))
