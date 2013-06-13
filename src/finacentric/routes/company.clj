*(ns finacentric.routes.company
  (:use compojure.core)
  (:use clojure.pprint)
  (:use finacentric.util
        finacentric.validation
        finacentric.forms)
  (:require [finacentric.views.layout :as layout]
            [finacentric.ajax :as ajax]
            [finacentric.routes.auth :as auth]
            [finacentric.validation-utils :as vali-util]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [finacentric.models.db :as db]
            [hiccup.core :as hiccup]
            [korma.core :as korma]))


(def ^:dynamic *context* nil)




(defn layout [& content]
  (layout/render
   "app/co_base.html" {:content (apply str (flatten content))}))

(defn prepare-invoices [from to page per-page]
  (db/get-invoices from to (db/page-filter page per-page)))


(defn dashboard [supplier-id buyer-id]
  (binding [*context* (str "/supplier/" supplier-id "/" buyer-id)]
    (layout/render
     "app/co_dashboard.html" {:context *context*
                       :invoices nil ;TODO
                       })))

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

(defn create-supplier-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (text-input :name "Nazwa firmy *" 60)
       ;; (text-input :email1 "Adres email *" 60)
       ;; (text-input :email2 "Adres email 2" 60)
       (text-input :addres_street "Ulica" 80)
       (text-input :addres_street_no "Numer budynku/lokalu" 20)
       (text-input :addres_zipcode "Kod pocztowy" 20)
       (text-input :addres_city "Miasto" 60)
       (text-input :nip "NIP" 10)
       (text-input :regon "REGON" 14)))))

(defvalidator validate-create-supplier-form
  (rule :name (<= 5 (count _) 60) "Nazwa musi mieć między 5 a 60 znaków.")
  ;; (rule :email1 (vali/is-email? _) "Niepoprawny format adresu email")
  ;; (rule :email1 (<= (count _) 50) "Email nie powinien mieć więcej niż 50 znaków")
  ;; (option :email2 (vali/is-email? _) "Niepoprawny format adresu email")
  ;; (option :email2 (<= (count _) 50) "Email nie powinien mieć więcej niż 50 znaków")
  (option :address_street (<= (count _) 80) "Ulica nie powinna mieć więcej niż 80 znaków")
  (option :address_street_no (<= (count _) 20) "Numer nie powinien mieć więcej niż 20 znaków")
  (option :nip (vali-util/is-nip? _) "Niepoprawny format NIP (proszę zapisać same cyfry, bez pauz)")
  (option :regon (vali-util/is-regon? _) "Niepoprawny format REGON")
  )



(defn FORM-add-supplier [company-id]
  (FORM "/add-supplier"
              #(layout (create-supplier-form %1 %2))
              validate-create-supplier-form
              #(let [id (db/create-supplier-for-company!
                         company-id
                         (dissoc % :email1 :email2)
                         (->> [:email1 :email2]
                              (map %)
                              (remove nil?)))]
                 (resp/redirect (str "supplier/" id)))))



(defroutes company-routes
  (context "/company" {:as request}
    (with-int-param [company-id (auth/get-current-users-company-id)]
      (routes-when (auth/logged-to-company? company-id)
        (FORM-add-supplier company-id)))))
  
