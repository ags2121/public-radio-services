(ns public-radio-services.handler
  (:require [compojure.core :refer [GET POST ANY defroutes]]
            [compojure.route :as route]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params         :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [get-header header]]
            [public-radio-services.services.visitor-count :as vc]
            [public-radio-services.services.news :as news]
            [public-radio-services.services.requests :as requests]
            [ring.adapter.jetty :as jetty]
            [liberator.core :refer [defresource]]
            [environ.core :refer [env]]
            [clojure.string :only [blank?] :as string]))

(declare post-request)
(declare get-requests)

(defroutes routes
           (GET "/news" []
             {:body {:news (news/get-news)}})

           (POST "/requests" [] post-request)
           (GET "/requests" [] (requests/get-requests))

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

(defresource post-request
             :allowed-methods [:post]
             :available-media-types ["application/json"]
             :processable?
             (fn [ctx]
               (let [params (get-in ctx [:request :params])
                     errors (requests/validate-request params)]
                 (if (empty? errors)
                   true
                   [false (assoc ctx ::errors errors)])))
             :handle-unprocessable-entity
             (fn [ctx] {::errors (::errors ctx)}) ; clojure keywords are functions and liberator passes each handler the context
             :post!
             (fn [ctx]
               (let [saved-request (requests/save-request! (get-in ctx [:request :params]))
                     request-id (-> saved-request :tempids vals first)]
                 {::request-id (str request-id)}))
            :handle-created
             (fn [ctx] {::id (::request-id ctx)}))

(defn wrap-allow-cors-credentials [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Access-Control-Allow-Credentials"] "true"))))

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
