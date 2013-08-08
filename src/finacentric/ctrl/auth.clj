(ns finacentric.ctrl.auth
  (:use [causeway.properties :only [defprop]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [finacentric.model.user])
  (:require [noir.session :as session]))


(defn is-logged-in? []
  (boolean (session/get :login-data)))

(defn log-in! [user password]
  (when-let [data (get-login-data user password)]
    (session/put! :login-data data)))

(defn log-out! []
  (session/remove! :login-data))

(defn get-default-url []
  (if (is-logged-in?)
    "/home"
    "/login"))
