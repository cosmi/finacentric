(ns finacentric.ctrl.auth
  (:use [causeway.properties :only [defprop]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [causeway.validation]
        [causeway.l10n :only [loc]]
        [finacentric.model users companies]
        [finacentric.links])
  (:require [noir.session :as session]
            [noir.validation :as vali]))


(defn is-logged-in? []
  (boolean (session/get :login-data)))

(defn log-in! [user password]
  (when-let [data (get-login-data user password)]
    (session/put! :login-data data)
    true))

(defn log-out! []
  (session/remove! :login-data))

(defn get-current-user-id []
  (-> (session/get :login-data)
      (get :id)))


(defn get-current-company-id []
  (->  (get-current-user-id) get-users-company-id)
  )

(defn partially-registered? []
  (and (is-logged-in?) (->  (get-current-user-id) get-users-company-id nil?)))

(defn get-current-company-mode []
  (-> get-current-company-id
      get-company-mode))

(defn login-mode []
  (if (not (is-logged-in?)) :not-logged-in
      (let [company-id (get-current-company-id)]
        (if (nil? company-id) :partial-registration
            (get-company-mode company-id)))))


(defn get-default-url []
  (case (login-mode)
    :buyer (link :buyer :dashboard)
    :supplier (link :supplier :dashboard)
    :partial-registration (link :register-supplier)
    :not-logged-in (link :login)))

(defvalidator password-validator
  (rule :password (<= 5 (count _) 50) (loc "Hasło powinno mieć 5 do 50 znaków"))
  (rule :password (re-matches #"[a-zA-Z0-9-_!@#$%^&*]*" _) (loc "Hasło zawiera niedozwolone znaki"))
  (rule :repeat-password (= (get-field :password) _) (loc "Hasła się nie zgadzają"))
  )

(defvalidator user-data-validator
  (optional
   (rule :first_name (<= 2 (count _) 30) (loc "Imię powinno mieć 2 do 30 znaków"))
   (rule :last_name (<= 2 (count _) 40) (loc "Nazwisko powinno mieć 2 do 40 znaków")))
  (rule :email (vali/is-email? _) (loc "Niepoprawny format adresu email"))
  (rule :email (<= (count _) 50) (loc "Email nie powinien mieć więcej niż 50 znaków")))

(defvalidator register-form-validator
  (call-validator password-validator)
  (call-validator user-data-validator))


(defn register-user! [data]
  (if-let [user-data (create-user-with-pass! (select-keys data [:first_name :last_name :email]) (data :password))]
    (log-in! (data :email) (data :password))
    (throw-validation-error :email (loc "Użytkownik z danym adresem email już istnieje"))))
