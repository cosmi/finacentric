(ns finacentric.handler
  (:use finacentric.routes.auth
        finacentric.routes.home
        [dieter.core :only [asset-pipeline]]
        [ring.middleware.file-info :only [wrap-file-info]]
        compojure.core)
  (:require [noir.util.middleware :as middleware]
            [noir.session :as session]
            [compojure.route :as route]
            [finacentric.models.schema :as schema]
            [taoensso.timbre :as timbre]
            [com.postspectacular.rotor :as rotor]
            [org.httpkit.server :as http-kit])))

(defroutes app-routes
  (route/resources "/")
  (route/not-found "Not Found"))

(defn init
  "runs when the application starts and checks if the database
   schema exists, calls schema/create-tables if not."
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info
     :enabled? true
     :async? false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn rotor/append})
  
  (timbre/set-config!
    [:shared-appender-config :rotor]
    {:path "finacentric.log" :max-size (* 512 1024) :backlog 10})
  
  (if-not (schema/actualized?)
    (schema/actualize))
  
  (timbre/info "finacentric started successfully"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  [] 
  (timbre/info "finacentric is shutting down..."))

(def config-options {:engine :v8              
                     :compress false
                     :asset-roots ["resources"]
                     :cache-root "resources/asset-cache"
                     :cache-mode :development
                     :log-level  :normal
                     :precompiles ["./assets/myfile.js.dieter"]})

(defn dupsko [app]
  (fn [req] (let [resp (app req)] (prn resp) resp)))

;;append your application routes to the all-routes vector
(def all-routes [auth-routes home-routes app-routes])
(def app (->
          (middleware/app-handler all-routes)
          (asset-pipeline config-options)))
(def war-handler app)

(defn dev? [args] (some #{"-dev"} args))

(defn port [args]
  (if-let [port (first (remove #{"-dev"} args))]
    (Integer/parseInt port)
    8080))

(defn -main [& args]
  (http-kit/run-server
    (if (dev? args) (reload/wrap-reload war-handler) war-handler)
    {:port (port args)})
  (timbre/info "server started on port"))
