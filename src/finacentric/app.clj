(ns finacentric.app
    (:use [compojure.core]
          [causeway.templates :only [render]]
          [causeway.l10n :only [loc]]
          [finacentric.routes.login :only [login-routes]]))

(defroutes public-routes
  #'login-routes)

