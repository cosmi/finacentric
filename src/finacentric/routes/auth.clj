(ns finacentric.routes.auth
  (:use compojure.core
        finacentric.util
        finacentric.validation
        finacentric.forms)
  (:require [finacentric.views.layout :as layout]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [finacentric.models.db :as db]
            [hiccup.core :as hiccup]))

(defvalidator valid-login
  (rule :id (or (= "admin" _) (vali/is-email? _)) "Niepoprawny adres e-mail")
  (rule :pass #(true) ""))

(defn login-form [input errors]
  (layout/render "app/login.html" {:input input :errors errors}))

(defn profile []
  (layout/render
   "profile.html"
   {:user (db/get-user (session/get :user-id))}))

(defn update-profile [{:keys [first-name last-name email]}]
  (db/update-user (session/get :user-id) first-name last-name email)
  (profile))

(defn- session-put-user [user]
  (session/put! :user-id (user :id)))

(defn handle-login [id pass]
  (let [user (db/find-user id)]
    (prn user id pass)
    (when (and user (crypt/compare pass (:pass user)))
      (session-put-user user))))

(defn logout []
  (session/clear!)
  (resp/redirect "/"))

(defroutes auth-routes

  (FORM "/login"
        (fn [input errors] (login-form input errors))
        valid-login
        (fn [input] (if (handle-login (input :id) (input :pass))
                     (resp/redirect "/")
                     (login-form (dissoc input :pass) {:email "Niepoprawny login lub hasło"}))))
  
  (GET "/logout" []
       (logout)))


;; Helpers

(defn logged-as-admin? []
  (db/is-admin? (session/get :user-id)))


(defn logged-to-company? [company-id]
  (db/user-to-company-access? (session/get :user-id) company-id))

(defn get-current-users-company-id []
  (db/get-users-company-id (session/get :user-id)))
