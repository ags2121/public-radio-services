(ns public-radio-services.handler
  (:require [compojure.core :refer [GET ANY defroutes]]
            [compojure.route :as route]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params         :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [public-radio-services.visitor-count :as vc]
            [public-radio-services.news :as news]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]))

(defroutes routes
  (GET "/visitor-count" {cookies :cookies}
    (let [{cookie :cookie count :count} (vc/get-visitor-count cookies)]
      {:cookies {vc/COOKIE-NAME cookie}
       :body    {:count count}}))
  (GET "/news" []
    {:body {:news (news/get-news)}})
  (route/not-found {:body {:suh :dude}}))

(def app (-> routes
             wrap-cookies
             wrap-params
             wrap-json-response))

(defn -main [& [port]]
  (let [port (Integer. ^int (or port (env :port) 5000))]
    (jetty/run-jetty #'app {:port port :join? false})))
