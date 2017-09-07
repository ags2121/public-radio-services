(ns public-radio-services.handler
  (:require [compojure.core :refer [GET POST ANY defroutes]]
            [compojure.route :as route]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.response :refer [get-header header redirect response]]
            [public-radio-services.services.visitor-count :as vc]
            [public-radio-services.services.fetcher :as f]
            [public-radio-services.scheduler :as scheduler]
            [ring.adapter.jetty :as jetty]
            [liberator.core :refer [defresource]]
            [environ.core :refer [env]]))


;; -- ROUTES ----------------------------------------------------------
;;
;;
;;
(defroutes public-routes
           (GET "/newscasts" []
             {:body {:newscasts (f/get-newscasts)}})

           (GET "/podcasts" []
             {:body {:podcasts (f/get-podcasts)}})

           (ANY "/visitor-count" {cookies :cookies request-method :request-method}
             (case request-method
               :get
               (let [{cookie :cookie count :count} (vc/get-visitor-count cookies)]
                 {:cookies {vc/COOKIE-NAME cookie}
                  :body    {:count count}})
               :post
               (let [response (future (vc/delete-visitor! cookies))] ; we dont care if this doesn't return
                 {})))

           (route/not-found {:body {:suhdude "https://vine.co/v/izX5WhPqIvi"}}))

(defroutes app-routes
           (-> public-routes
               wrap-json-response))

;; -- CUSTOM MIDDLEWARE ----------------------------------------------------------
;;
;;
;;
(defn wrap-allow-cors-credentials [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Access-Control-Allow-Credentials"] "true"))))

(def app (-> app-routes
             (wrap-cors identity)
             wrap-cookies
             wrap-params
             wrap-allow-cors-credentials))

(defn init []
  (dotimes [_ (Math/ceil (/ (count f/ENDPOINTS) f/STEP))] (f/add-to-cache))
  (scheduler/pre-cache-scheduler))

(defn -main [& [port]]
  (let [port (Integer. ^int (or port (env :port) 5000))]
    (dotimes [_ (Math/ceil (/ (count f/ENDPOINTS) f/STEP))] (f/add-to-cache))
    (jetty/run-jetty #'app {:port port :join? false})
    (println "app start")
    (scheduler/pre-cache-scheduler)
    ))
