(ns finacentric.routes.buyer
  (:use [compojure.core]
        [causeway.utils]
        [finacentric.ctrl.auth]
        [finacentric.ctrl.company]
        [finacentric.render :only [render]]
        [causeway.validation :only [validates? get-errors validate-let]])
  (:require [ring.util.response :as response]))


(defroutes buyer-routes
  (context "/buyer" []
    (GET "/dashboard" []
      (render "buyer_dashboard.html" {})
      )

    (GET "/add-supplier" []
      (render "buyer_add_supplier.html" {})
      )

    (POST "/add-supplier" {:keys [params]}
      (render "buyer_add_supplier.html" {})
      )




    ))
