(defproject public-radio-services "0.1.0-SNAPSHOT"
  :description "A collection of services for publicradio.info"
  :url "https://github.com/radioopensource/public-radio-services"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring-cors "0.1.7"]
                 [compojure "1.4.0"]
                 [clj-time "0.11.0"]
                 [ring/ring-json "0.4.0"]
                 [http-kit "2.1.18"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.json "0.2.6"]
                 [environ "1.0.1"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.9.7"]]
  :ring {:handler public-radio-services.handler/app
         :auto-reload? true
         :nrepl {:start? true
                 :port 9998}}
  :uberjar-name "public-radio-services-standalone.jar"
  :profiles
    {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                        [ring/ring-mock "0.3.0"]]}
     :production {:env {:production true}}})
