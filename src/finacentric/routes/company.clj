(ns finacentric.routes.company
  (:use compojure.core)
  (:use clojure.pprint)
  (:use finacentric.util
        finacentric.validation
        finacentric.forms)
  (:require [finacentric.views.layout :as layout]
            [finacentric.models.invoices :as invoices]
            [finacentric.files :as files]
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



(defn layout [& content]
  (layout/render
   "app/co_base.html" {:content (apply str (flatten content))}))

(defn prepare-invoices [from to page per-page]
  (db/get-invoices from to (db/page-filter page per-page)))


(defn dashboard [supplier-id invoices sort-column sort-dir]
  (prn sort-column sort-dir)
  (layout/render
   "app/co_dashboard.html" {:invoices invoices
                            :s {:col (name sort-column)
                                :dir (clojure.string/lower-case (name sort-dir))}}))

(defn form-wrapper [content]
  (hiccup/html [:form {:method "post"}
                [:fieldset content]
                [:button {:type "submit" :class "btn"} "OK"]]))

;; Invoice Details

(defn invoice-view [invoice-id invoice]
  (layout/render
   "app/co_invoice.html" {:i (assoc invoice :id invoice-id)}))

(defn invoice-details [company-id invoice-id]
  (when-let [invoice (db/get-invoice-for-company invoice-id company-id)]
    (invoice-view invoice-id invoice)))

(defn invoice-file [company-id invoice-id]
  (when-let [invoice (db/get-invoice-for-company invoice-id company-id)]
    (when (invoice :file_id)
      (when-let [file (files/get-file (invoice :file_id))]
        (files/response file (str (invoice :number) ".pdf"))))))

;; Forms & Stuff

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
  (optional
   (rule :address_street (<= (count _) 80) "Ulica nie powinna mieć więcej niż 80 znaków")
   (rule :address_street_no (<= (count _) 20) "Numer nie powinien mieć więcej niż 20 znaków")
   (rule :nip (vali-util/is-nip? _) "Niepoprawny format NIP (proszę zapisać same cyfry, bez pauz)")
   (rule :regon (vali-util/is-regon? _) "Niepoprawny format REGON")))




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


(defvalidator validate-offer-discount-form
  (date-field :earliest_discount_date  "Błędny format daty")
  (decimal-field :annual_discount_rate 4 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 4 miejsca po przecinku")
  (rule :annual_discount_rate (< _ 100) "Wartość nie może przekraczać 100%")
  )

(defn offer-discount-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (decimal-input :annual_discount_rate "Roczny zarobek (%)" 40)
       (date-input :earliest_discount_date "Najwcześniejsza możliwa data płatności" 30)
       ))))




(defn FORM-offer-discount [company-id invoice-id]
  (FORM "/offer-discount"
        (fn [input errors]
          (let [input (if-not (nil? input)
                        input
                        (db/get-invoice-unchecked invoice-id))]
            (layout (offer-discount-form input errors))))
        validate-offer-discount-form
        #(do (prn %)
             (invoices/invoice-offer-discount!
              company-id invoice-id
              (% :annual_discount_rate)
              (% :earliest_discount_date))
             (resp/redirect "."))))

(def SORT-COLUMNS {:name :companies.name
                   :number :number
                   :payment_date :payment_date
                   :issue_date :issue_date
                   :net_total :net_total
                   :gross_total :gross_total})
(def SORT-DIRS {:desc :DESC
                :asc :ASC})

(defn hello [company-id sort-column sort-dir]
  (with-pagination page-no
    (with-page-size 30 page-size
      (dashboard company-id
                 (->>
                  (db/get-invoices-with-suppliers company-id
                                                  (db/page-filter page-no page-size)
                                                  (db/sorted-by sort-column sort-dir)
                                                  invoices/not-rejected-filter)
                  (map invoices/append-state))
                 sort-column
                 sort-dir))))

(defroutes company-routes
  (context "/company" {:as request}
    (with-int-param [company-id (auth/get-current-users-company-id)]
      (routes-when (auth/logged-to-company? company-id)
        (FORM-add-supplier company-id)
        (GET "/hello" [sort dir]
          (let [sort (or (get SORT-COLUMNS (keyword sort)) :id)
                dir (or (get SORT-DIRS (keyword dir)) :ASC)]
            (hello company-id sort dir)))
        (context "/invoice" []
          (id-context invoice-id
                      (routes-when (invoices/check-invoice
                                    invoice-id
                                    (invoices/is-buyer? company-id))
                        (GET "/" []
                          (invoice-details company-id invoice-id))
                        (GET "/file" []
                          (invoice-file company-id invoice-id))
                        
                        (routes-when (invoices/check-invoice invoice-id
                                                             (invoices/has-state? :accepted :input))
                          (POST "/accept" []
                            (invoices/invoice-accept! company-id invoice-id))
                          (POST "/reject" []
                            (invoices/invoice-reject! company-id invoice-id)))
                        (routes-when (invoices/check-invoice invoice-id
                                                             (invoices/has-state? :accepted))
                          (POST "/accept-cancel" []
                            (invoices/invoice-accept-cancel! company-id invoice-id)))
                        (routes-when (invoices/check-invoice invoice-id
                                                             (invoices/has-state? :rejected))
                          (POST "/reject-cancel" []
                            (invoices/invoice-reject-cancel! company-id invoice-id)))
                        (routes-when (invoices/check-invoice invoice-id
                                                             (invoices/has-state? :accepted :discount_offered))
                          (FORM-offer-discount company-id invoice-id))
                        (routes-when (invoices/check-invoice invoice-id
                                                             (invoices/has-state? :discount_accepted))
                          (POST "/confirm-discount" [date]
                            (let [date (parse-date date)]
                              (invoices/invoice-confirm-discount! company-id invoice-id date))))
                        (routes-when (invoices/check-invoice invoice-id
                                                             (invoices/has-state? :discount_confirmed))
                          (POST "/cancel-confirm-discount" []
                            (invoices/invoice-cancel-confirm-discount! company-id invoice-id)))

                        )))))))
