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
    (context "/suppliers" []
      (GET "/list" []
        (render "buyer_suppliers_list.html" {})
        )
      (GET "/invites" []
        (render "buyer_suppliers_invites.html" {})
        ))
    (context "/invoices" []
      (GET "/list" []

        )
      (GET "/search" []

        )

      )

    (context "/ajax" []
      (GET "/suppliers" [page sorted]
        (let [page (Integer/parseInt (or page "1"))]
          (render "ajax/buyer_ajax_suppliers.html" {:page page :sorted sorted
                                               :suppliers (get-current-suppliers-list (dec page) sorted)})
        ))
      (GET "/regcodes" [page sorted]
        (let [page (Integer/parseInt (or page "1"))]
          (render "ajax/buyer_ajax_regcodes.html" {:page page :sorted sorted
                                               :regcodes (get-current-regcodes-list (dec page) sorted)})
        )
        )
      )))

