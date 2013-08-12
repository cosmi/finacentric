(ns finacentric.app
    (:use [compojure.core]
          [causeway.templates :only [render]]
          [causeway.l10n :only [loc]]
          [finacentric.ctrl.auth :only [login-mode]]
          [finacentric.routes.login :only [login-routes]]
          [finacentric.routes.supplier :only [supplier-routes]]
          [finacentric.routes.buyer :only [buyer-routes]]))

(defroutes public-routes
  (context "" []
    #'login-routes
    ;; (case (login-mode)
    ;;   :supplier
      #'supplier-routes
      ;; :buyer
      #'buyer-routes
      ;; (constantly nil))))
))
