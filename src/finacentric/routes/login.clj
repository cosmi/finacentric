(ns finacentric.routes.login
  (:use [compojure.core]
        [finacentric.ctrl.auth]
        [causeway.templates :only [render]])
  (:require [ring.util.response :as response]))



(defroutes login-routes
  (GET "/login" []
    (render "login.html" {}))
  (POST "/login" [login password]
    (if-not (log-in! login password)
      (render "login.html" {:error :wrong-pass})
      (response/redirect (get-default-url)))))
