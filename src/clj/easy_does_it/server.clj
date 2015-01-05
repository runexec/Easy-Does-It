(ns easy-does-it.server
  (:gen-class)
  (:require [clojure.java.io :as io]
            [easy-does-it.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.reload :refer [auto-reload]]
            [ring.middleware.reload :as reload]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [cognitect.transit :as transit]))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(def save-file "easy-does-it.edn")

(defroutes routes
  (resources "/")
  (resources "/react" {:root "react"})
  (POST "/save"
        req
        (spit save-file
              (pr-str
               (transit/read
                (transit/reader (:body req) :json))))
        "Saved!")
  (GET "/load"
       req
       (let [out-stream (java.io.ByteArrayOutputStream. 4096)
             w (transit/writer out-stream :json)]
         (transit/write w 
                        (read-string (slurp save-file)))
         (.toString out-stream)))
  (GET "/*" req (page)))

(def http-handler
  (if is-dev?
    (reload/wrap-reload (wrap-defaults #'routes api-defaults))
    (wrap-defaults routes api-defaults)))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 10555))]
    (print "Starting web server on port" port ".\n")
    (run-server http-handler {:port port :join? false})))

(defn run-auto-reload [& [port]]
  (auto-reload *ns*)
  (start-figwheel))

(defn run [& [port]]
  (when is-dev?
    (run-auto-reload))
  (run-web-server port))

(defn -main [& [port]]
  (run port))
