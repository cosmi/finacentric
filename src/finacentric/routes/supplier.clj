(ns finacentric.routes.supplier
  (:use [compojure.core]
        [causeway.utils]
        [finacentric.ctrl.auth]
        [finacentric.ctrl.company]
        [finacentric.render :only [render]]
        [causeway.validation :only [validates? get-errors validate-let]])
  (:require [ring.util.response :as response]))


(defroutes supplier-routes
  (context "/supplier" []
    (GET "/dashboard" []
      (render "supplier_dashboard.html" {})
      )
    

    ))
