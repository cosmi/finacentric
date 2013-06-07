(ns finacentric.routes.supplier
  (:use compojure.core)
  (:use clojure.pprint)
  (:use finacentric.util
        finacentric.validation
        finacentric.forms)
  (:require [finacentric.views.layout :as layout]
            [finacentric.ajax :as ajax]
            [finacentric.routes.auth :as auth]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [finacentric.validation-utils :as vali-util]
            [noir.util.crypt :as crypt]
            [finacentric.models.db :as db]
            [hiccup.core :as hiccup]
            [korma.core :as korma]))


(def ^:dynamic *supplier-id* nil)
(def ^:dynamic *buyer-id* nil)
(def ^:dynamic *context* nil)

(defn current-supplier-id []
  *supplier-id*)
(defn current-buyer-id []
  *buyer-id*)



(defn layout [& content]
  (layout/render
   "layout.html" {:content (apply str (flatten content))}))

(defn prepare-invoices [from to page per-page]
  (db/get-invoices from to (db/page-filter page per-page)))


(defn dashboard [supplier-id buyer-id]
  (binding [*supplier-id* supplier-id
            *buyer-id* buyer-id
            *context* (str "/supplier/" supplier-id "/" buyer-id)]
    (layout/render
     "dashboard.html" {:context *context*
                       :invoices (prepare-invoices (current-supplier-id) (current-buyer-id) 0 100)})))

(defn form-wrapper [content]
  (hiccup/html [:form {:method "post"}
                [:fieldset content]
                [:button {:type "submit" :class "btn"} "OK"]]))



(defvalidator valid-simple-invoice
  (rule :number (<= 2 (count _) 40) "Numer powinno mieć 2 do 40 znaków")
  (rule :issue_date (<= 2 (count _) 30) "Nazwisko powinno mieć 2 do 40 znaków")
  (rule :payment_date (<= 2 (count _) 30) "Nazwisko powinno mieć 2 do 40 znaków")
  (rule :net_total (<= (count _) 50) "Email nie powinien mieć więcej niż 50 znaków")
  (rule :gross_total (<= (count _) 50) "Email nie powinien mieć więcej niż 50 znaków")
  )

(defn simple-invoice-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (text-input :number "Numer" 40)
       (date-input :issue_date "Data wystawienia" 30)
       (date-input :payment_date "Data wystawienia" 30)
       (decimal-input :net_total "Wartość netto" 30)
       (decimal-input :gross_total "Wartość brutto" 30)
       ))))


;;; REGISTER FROM REG-CODE
(defvalidator register-from-reg-code-validator
  (rule :email (vali/is-email? _) "Niepoprawny format adresu email")
  (rule :email (<= (count _) 50) "Email nie powinien mieć więcej niż 50 znaków")
  (option :first_name (<= 2 (count _) 30) "Imię powinno mieć 2 do 30 znaków")
  (option :last_name (<= 2 (count _) 40) "Nazwisko powinno mieć 2 do 40 znaków")
  (rule :password (<= 5 (count _) 50) "Hasło powinno mieć 5 do 50 znaków")
  (rule :password (re-matches #"[a-zA-Z0-9-_!@#$%^&*]*" _) "Hasło zawiera niedozwolone znaki")
  (rule :repeat-password (= (get-field :password) _) "Hasła się nie zgadzają")
  (rule :reg-code (< 0 (count _)) "Pole obowiązkowe"))

(defn register-from-reg-code-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (text-input :reg-code "Kod rejestracyjny *" 50)
       (text-input :email "Adres email *" 50)
       (pass-input* :password "Hasło *" 50)
       (pass-input* :repeat-password "Powtórz hasło *" 50)
       (text-input :first_name "Imię" 30)
       (text-input :last_name "Nazwisko" 40)))))

(defn FORM-register-to-company []
  (FORM "/register"
        #(layout (register-from-reg-code-form %1 %2))
        register-from-reg-code-validator
        #(let [id (errors-validate
                      "Niepoprawny kod rejestracyjny"
                    (db/create-user-from-reg-code!
                     (% :reg-code)
                     (dissoc % :reg-code :repeat-password)))]
           (resp/redirect (str "supplier")))))






(defroutes supplier-routes
  (context "/supplier" {:as request}
    (GET "/" []
      ;; tu będzie pętla przekierowań jak gość nie ma ustawionego company_id
      (resp/redirect (str "/supplier/" (auth/get-current-users-company-id)))) 
    
    
    (id-context supplier-id
      (id-context buyer-id
        (GET "/hello" []
          (dashboard supplier-id buyer-id))
        (GET "/simple-invoice-form" []
          (layout
           (simple-invoice-form {} {})))
        (POST "/simple-invoice-form" {params :params :as request}
          (if-let [obj (validates? valid-simple-invoice params)]
                (and (db/simple-create-invoice obj supplier-id buyer-id)
                     (resp/redirect "hello"))
                (layout
                 (simple-invoice-form params (get-errors)))))

        ))))


