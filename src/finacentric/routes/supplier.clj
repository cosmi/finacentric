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


(defn dashboard [supplier-id buyer-id page per-page]
  ;(binding [*context* (str "/supplier/" supplier-id "/" buyer-id)]
  (layout/render
   "dashboard.html" {:invoices (db/get-invoices
                                supplier-id buyer-id
                                (db/page-filter page per-page))}))

(defn form-wrapper [content]
  (hiccup/html [:form {:method "post"}
                [:fieldset content]
                [:button {:type "submit" :class "btn"} "OK"]]))



(defvalidator valid-simple-invoice
  (rule :number (<= 2 (count _) 40) "Numer powinno mieć 2 do 40 znaków")
  (date-field :issue_date "Błędny format daty")
  (date-field :sell_date "Błędny format daty")
  (date-field :payment_date "Błędny format daty")
  (rule :net_total (<= (count _) 50) "Zbyt długi ciąg")
  (rule :gross_total (<= (count _) 50) "Zbyt długi ciąg")
  (decimal-field :net_total 2 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")
  (decimal-field :gross_total 2 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")
  )

(defn simple-invoice-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (text-input :number "Numer" 40)
       (date-input :issue_date "Data wystawienia" 30)
       (date-input :sell_date "Data sprzedaży" 10)
       (date-input :payment_date "Termin płatności" 30)
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
        (fn [input errors] (layout (register-from-reg-code-form input errors)))
        register-from-reg-code-validator
        #(let [id (errors-validate
                      :reg-code
                      "Niepoprawny kod rejestracyjny"
                    (db/create-user-from-reg-code!
                     (% :reg-code)
                     (dissoc % :reg-code :repeat-password :password)
                     (% :password)))]
           (resp/redirect (str "supplier")))
        "."
        ))

(defn FORM-simple-invoice [supplier-id buyer-id]
  (FORM "/simple-invoice-form"
        #(layout (simple-invoice-form %1 %2))
        valid-simple-invoice
        #(db/simple-create-invoice % supplier-id buyer-id)
        "hello"))




(defroutes supplier-routes
  (context "/supplier" {:as request}
    (GET "/" []
      (resp/redirect (str "/supplier/" (or (auth/get-current-users-company-id) 0)))) 
    (FORM-register-to-company)
    (id-context supplier-id
      (routes-when (auth/logged-to-company? supplier-id)
        (id-context buyer-id
          (GET "/hello" []
            (with-pagination page-no
              (with-page-size 30 page-size
                (dashboard supplier-id buyer-id page-no page-size))))
          (FORM-simple-invoice supplier-id buyer-id)
          )))))


