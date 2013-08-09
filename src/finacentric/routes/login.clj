(ns finacentric.routes.login
  (:use [compojure.core]
        [causeway.utils]
        [finacentric.ctrl.auth]
        [finacentric.ctrl.company]
        [finacentric.render :only [render]]
        [causeway.validation :only [validates? get-errors validate-let]])
  (:require [ring.util.response :as response]))



(defroutes login-routes
  (context "" []
    (GET "/login" []
      (render "login.html" {}))
    (POST "/login" [login password]
      (if-not (log-in! login password)
        (render "login.html" {:error :wrong-pass})
        (response/redirect (get-default-url))))
    
    (GET "/register" []
      (render "register.html" {}))
    (POST "/register" {form :params}
      (validate-let [data (validates? register-form-validator form)]
                    (do
                      (register-user! data)
                      (response/redirect (get-default-url)))
                    (render "register.html" {:error (get-errors) :form form})))
    (routes-when (partially-registered?)
      (GET "/register-company" []
        (render "register_company.html" {}))
      (POST "/register-company" {form :params}
        (validate-let [data (validates? register-company-validator form)]
                      (do
                        (register-company! data)
                        (response/redirect (get-default-url)))
                      (render "register_company.html" {:error (get-errors) :form form}))))))

