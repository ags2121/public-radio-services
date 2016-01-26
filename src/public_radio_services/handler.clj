(ns public-radio-services.handler
  (:require [compojure.core :refer [GET ANY defroutes]]
            [compojure.route :as route]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params         :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [get-header header]]
            [public-radio-services.services.visitor-count :as vc]
            [public-radio-services.services.news :as news]
            [ring.adapter.jetty :as jetty]
            [environ.core :refer [env]]))

(defroutes routes
  (GET "/visitor-count" {cookies :cookies}
    (let [{cookie :cookie count :count} (vc/get-visitor-count cookies)]
      {:cookies {vc/COOKIE-NAME cookie}
       :body    {:count count}}))
  (GET "/news" []
    {:body {:news (news/get-news)}})
  (route/not-found {:body {:suhdude "https://vine.co/v/izX5WhPqIvi"}}))

(defn wrap-allow-cors-credentials [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers  "Access-Control-Allow-Credentials"] "true"))))

(def app (-> routes
             (wrap-cors :access-control-allow-origin [#"http://localhost(:\d{2,4})" #"https?://www.publicradio.info"]
                        :access-control-allow-methods [:get :post])
             wrap-allow-cors-credentials
             wrap-cookies
             wrap-params
             wrap-json-response))

(defn -main [& [port]]
  (let [port (Integer. ^int (or port (env :port) 5000))]
    (jetty/run-jetty #'app {:port port :join? false})))
