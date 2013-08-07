(ns finacentric.handler
  (:gen-class)
  (:use [causeway.properties :only [defprop prop-panel]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [compojure.route :only [not-found resources]]
        [compojure.core :only [context defroutes routes]]
        [causeway.utils :only [routes-when]]
        [causeway.validation :only [wrap-validation]]
        [causeway.assets]
        [causeway.assets.providers]
        [causeway.templates :only [set-default-url-templates-provider!]]
        [finacentric.ctrl.auth :only [is-logged-in?]]
        [finacentric.app :only [public-routes logged-routes]]
        [finacentric.admin :only [admin-routes]])
  (:require [compojure.handler :as handler]
            [finacentric.localized]
            [causeway.assets.handlers :as handlers]
            [causeway.templates.preview :as preview]
            [causeway.middleware :as middleware]))



(def lesscss-handler 
    (handlers/lesscss-handler "precompiled/css"
                           ["sample.css"]))
(def sass-handler 
    (handlers/sass-handler "precompiled/css"
                           ["sample1.css"]))

(def coffee-script-handler 
    (handlers/coffee-script-handler "precompiled/js"
                                    ["sample.js"]))

(defroutes precompiled-resource-routes
  (context "/css" []
    lesscss-handler
    sass-handler)
  (context "/js" []
    coffee-script-handler))

(defroutes resource-routes
  precompiled-resource-routes
  (let [provider (combine-providers
                  ;; TODO: Remove if you don't use variants
                  (variant-provider "variants" "public")
                  (resource-provider "public"))]
  (-> provider
      (cond->
       (not (devmode?))
       (->
        (wrap-processor (yui-css-compressor) "css" "css")
        (wrap-processor (uglify-js-compressor) "js" "js")
        wrap-resource-lookup-caching))
      resource-handler)))

(def template-preview
  (preview/preview-templates-handler "templates"))


(defn wrap-access-fn [handler test-fn]
  ;; TODO: use one in causeway after deploying new version
  #(if (test-fn) (handler %) (constantly nil)))

(def main-handler
  (-> 
   (routes
     #'public-routes
     (wrap-access-fn #(is-logged-in?)
       #'logged-routes)
     #'admin-routes
     (routes-when (devmode?)
       (context "/template" []
           template-preview))
     resource-routes
     (not-found "Not Found"))
   ;; TODO: Add something like that:
   ;; (wrap-variant-selector (constantly :en))
   middleware/wrap-app-handler
   ))

(defn init []
  (alter-var-root #'*read-eval* (constantly false))
  (set-default-url-templates-provider!
   (combine-providers
    (variant-provider "variants" "templates")
    (resource-provider "templates")))
  ;; (when devmode?
  ;;   (require 'finacentric.devtools))
  )

(defn destroy [])
