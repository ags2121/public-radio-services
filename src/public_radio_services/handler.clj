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
            [public-radio-services.services.fetcher :as news]
            [public-radio-services.services.requests :as requests]
            [public-radio-services.scheduler :as scheduler]
            [ring.adapter.jetty :as jetty]
            [liberator.core :refer [defresource]]
            [environ.core :refer [env]]
            [public-radio-services.services.db :as db]
            [hiccup.core :as h]
            [clojure.string :as string]))

;; -- VIEWS ----------------------------------------------------------
;;
;;
;;
(defn login-form
  ([]
   (login-form "TRY"))
  ([banner]
   (h/html
     [:div
      [:div.banner banner]
      [:form {:action "/login" :method "post"}
       [:input {:name "password" :type "password"}]
       [:button "Submit"]]])))

(def requests
  (h/html
    [:ul
     (for [req (requests/get-requests)]
       [:li {:style "margin-bottom: 1em;"}
        (for [entry req]
          [:div
           [:span {:style "margin-right: 1em; color: darkgrey;"} (key entry)]
           [:span (val entry)]])
        ])]))

;; -- ROUTES ----------------------------------------------------------
;;
;;
;;

(declare post-request)
(declare get-requests)

(defroutes public-routes
           (GET "/newscasts" []
             {:body {:newscasts (news/get-newscasts)}})

           (GET "/podcasts" []
             {:body {:podcasts (news/get-podcasts)}})

           (POST "/requests" [] post-request)

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

(defn do-login
  "Check the submitted form data and update the session if necessary"
  [params session]
  (if (db/is-admin? (get params "password"))
    (assoc session :user "admin")
    session))

(defroutes secured-routes
           (POST "/login" {{referer "referer"} :headers params :form-params session :session}
             (.println System/out "START")
             (let [session (do-login params session)
                   redirect-url (if referer
                                  (as-> referer $ (string/split $ #"/") (last $) (str "/" $))
                                  "/requests")]
               (.println System/out redirect-url)
               (.println System/out session)
               (assoc (redirect redirect-url) :session session)))

           (GET "/requests" {session :session}
             (.println System/out "REQUESTS")
             (if (:user session)
               requests
               (login-form))))

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
             (fn [ctx] {::errors (::errors ctx)})           ; clojure keywords are functions and liberator passes each handler the context
             :post!
             (fn [ctx]
               (let [saved-request (requests/save-request! (get-in ctx [:request :params]))
                     request-id (:id saved-request)]
                 {::request-id (str request-id)}))
             :handle-created
             (fn [ctx] {::id (::request-id ctx)}))

(defroutes app-routes
           (-> secured-routes
               wrap-session)
           (-> public-routes
               wrap-json-response))

(defn wrap-allow-cors-credentials [handler]
  (fn [request]
    (let [response (handler request)]
      (assoc-in response [:headers "Access-Control-Allow-Credentials"] "true"))))

(def app (-> app-routes
             (wrap-cors :access-control-allow-origin [#"http://localhost(:\d{2,4})" #"https?://www.publicradio.info"]
                        :access-control-allow-methods [:get :post])
             wrap-cookies
             wrap-params
             wrap-allow-cors-credentials))

(defn -main [& [port]]
  (db/migrate)
  (let [port (Integer. ^int (or port (env :port) 5000))]
    (jetty/run-jetty #'app {:port port :join? false})
    (scheduler/poll-server)))
