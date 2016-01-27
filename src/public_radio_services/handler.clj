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
            [ring.adapter.jetty :as jetty]
            [liberator.core :refer [defresource]]
            [environ.core :refer [env]]))

(defn validate-request [{podcast-info "podcast-info" podcast-url "podcast-url"}]
  (let [errors {}
        errors (conj errors (if (clojure.string/blank? podcast-info)
                                  {::podcast-info ::not-present}
                                  {} ))
        errors (conj errors (if (and
                                  (not (clojure.string/blank? podcast-url))
                                  (empty? (re-matches #"(.*?).(mp3|MP3|rss|RSS)" podcast-url)))
                              {::podcast-url ::not-valid}
                              {} ))]
    errors))

(defresource request
             :allowed-methods [:post]
             :available-media-types ["application/json"]
             :processable?
             (fn [ctx]
               (let [params (get-in ctx [:request :params])
                     errors (validate-request params)]
                 (if (empty? errors)
                   true
                   [false (assoc ctx ::errors errors)])))
             :handle-unprocessable-entity
             (fn [ctx]
               (::errors ctx))
             :post!
             (fn [ctx]
               {:yo :bro})
            :handle-created
            (fn [ctx]
              {:yo :bro}))

(defroutes routes
  (ANY "/visitor-count" {cookies :cookies request-method :request-method}
    (case request-method
      :get
      (let [{cookie :cookie count :count} (vc/get-visitor-count cookies)]
        {:cookies {vc/COOKIE-NAME cookie}
         :body    {:count count}})
      :post
      (let [response (future (vc/delete-visitor! cookies))] ; we dont care if this doesn't return
        {})))

  (GET "/news" []
    {:body {:news (news/get-news)}})

  (POST "/request" [] request)

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
