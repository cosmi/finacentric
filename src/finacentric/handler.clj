(ns finacentric.handler
  (:use finacentric.routes.auth
        finacentric.routes.home
        finacentric.routes.admin
        [dieter.core :only [asset-pipeline]]
        [ring.middleware.file-info :only [wrap-file-info]]
        compojure.core)
  (:require [noir.util.middleware :as middleware]
            [noir.session :as session]
            [compojure.route :as route]
            [finacentric.models.schema :as schema]
            [taoensso.timbre :as timbre]
            [ring.middleware.reload :as reload]
            [com.postspectacular.rotor :as rotor]
            [org.httpkit.server :as http-kit]
            finacentric.validation))

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


(defn asset-fixpath-newuri [uri]
  (when (re-matches #"^/assets/.*" uri)
    (cond (re-matches #".*\.js" uri) (str uri ".coffee")
          (re-matches #".*\.css" uri) (clojure.string/replace uri ".css" ".less"))))

(def asset-fixpath
  (fn [app]
    (fn [req]
      (if-let [new-uri (asset-fixpath-newuri (req :uri))]
        (app (assoc req :uri new-uri))
        (app req)))))

;;append your application routes to the all-routes vector
(def all-routes [auth-routes home-routes admin-routes app-routes ])
(def app (->
          (middleware/app-handler all-routes)
          (finacentric.validation/wrap-validation)
          (asset-pipeline config-options)
          asset-fixpath))
(def war-handler app)

(defn dev? [args] (some #{"-dev"} args))

(defn port [args]
  (if-let [port (first (remove #{"-dev"} args))]
    (Integer/parseInt port)
    3000))

(defn -main [& args]
  (init)
  (http-kit/run-server
   ;(if (dev? args)
     (reload/wrap-reload #(apply #'war-handler %&))
    ; war-handler)
   {:port (port args)})
  (timbre/info "server started on port" port))
