(defproject public-radio-services "0.1.0-SNAPSHOT"
  :description "A collection of services for publicradio.info"
  :url "https://github.com/radioopensource/public-radio-services"
  :license {:name "MIT"
            :url  "http://opensource.org/licenses/MIT"}
  :dependencies [
                 [org.clojure/clojure "1.8.0"]
                 ;[com.amazonaws/aws-java-sdk-dynamodb "1.9.39" :exclusions [joda-time]]
                 ;[com.datomic/datomic-pro "0.9.5344"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [liberator "0.14.0"]
                 [ring-cors "0.1.7"]
                 [compojure "1.4.0"]
                 [clj-time "0.11.0"]
                 [ring/ring-json "0.4.0"]
                 [http-kit "2.1.18"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/data.json "0.2.6"]
                 [environ "1.0.1"]
                 [overtone/at-at "1.2.0"]
                 [org.clojure/data.xml "0.1.0-beta1"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.1"]
                 [postgresql/postgresql "9.1-901-1.jdbc4"]
                 [hiccup "1.0.5"]
                 [buddy/buddy-auth "1.2.0"]
                 [buddy/buddy-hashers "1.0.0"]
                 ]
  ;:repositories {"my.datomic.com" {:url      "https://my.datomic.com/repo"
  ;                                 :username :env/datomic_username
  ;                                 :password :env/datomic_password}}

  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.9.7"]
            [lein-environ "1.0.1"]]
  :ring {:handler      public-radio-services.handler/app
         :auto-reload? true
         :nrepl        {:start? true
                        :port   9998}}
  :uberjar-name "public-radio-services-standalone.jar"
  :profiles
  {:dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :project/dev   {:env {:dev        true
                         :port       3000
                         :nrepl-port 7000}}
   :project/test  {:env {:test       true
                         :port       3001
                         :nrepl-port 7001}
                   :dependencies
                        [[javax.servlet/javax.servlet-api "3.1.0"]
                         [ring/ring-mock "0.3.0"]]}
   :profiles/dev  {}
   :profiles/test {}
   :uberjar       {:aot :all}})
