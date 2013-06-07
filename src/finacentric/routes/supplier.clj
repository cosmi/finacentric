(ns finacentric.routes.supplier
  (:use compojure.core)
  (:use clojure.pprint)
  (:use finacentric.util
        finacentric.validation
        finacentric.forms)
  (:require [finacentric.views.layout :as layout]
            [finacentric.ajax :as ajax]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
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



(defroutes supplier-routes
  (context "/supplier" {:as request}
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


