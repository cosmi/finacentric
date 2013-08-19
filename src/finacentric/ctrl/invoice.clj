(ns finacentric.ctrl.invoice
  (:use [causeway.properties :only [defprop]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [causeway.validation]
        [causeway.l10n :only [loc]]
        [finacentric.model companies users regcodes invoices]
        [finacentric.ctrl auth]
        [finacentric.links]
        [finacentric.utils.validation]
        [korma.db :only [transaction rollback]])
  (:require [noir.session :as session]
            [noir.validation :as vali]
            [causeway.status :as status]))

(defprop INVOICE-FILE-SIZE-LIMIT 1000000 :doc "Maksymalny rozmiar pliku uploadowanej faktury w bajtach")


(defvalidator upload-invoice-validator
  (doseq [f [:number :issue_date :payment_date :net_total :gross_total]]
          (rule f (not-empty _) ""))
  (rule :number (<= 2 (count _) 40) "Numer powinien mieć 2 do 40 znaków")
  (date-field :issue_date "Błędny format daty")
  (date-field :payment_date "Błędny format daty")
  (rule :net_total (<= (count _) 50) "Zbyt długi ciąg")
  (rule :gross_total (<= (count _) 50) "Zbyt długi ciąg")
  (decimal-field :net_total 2 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")
  (decimal-field :gross_total 2 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")
  (convert :invoice (if (vali/valid-file? _) _ nil))
  (optional
      (rule :invoice (= "application/pdf" (:content-type _)) "Niepoprawny typ dokumentu")
      (rule :invoice (<= (:size _) @INVOICE-FILE-SIZE-LIMIT)
            "Plik z fakturą jest zbyt duży")))



(defn post-invoice! [data]
  (transaction
   (let [invoice (create-invoice! (get-current-company-id) (get-current-buyer-id) data)]
     (when (data :invoice)
       (upload-invoice-file! (invoice :id) (data :invoice))))))

(defn remove-invoice! [invoice-id]
  (transaction
   (if (check-invoice-auth invoice-id (get-current-company-id) :delete)
     (delete-invoice! invoice-id)
     (throw status/forbidden))))

(defn remove-invoice-upload! [invoice-id]
  (transaction
   (if (check-invoice-auth invoice-id (get-current-company-id) :delete-file)
     (delete-invoice-file! invoice-id)
     (throw status/forbidden))))



(defn verify-invoice! [invoice-id]
  (transaction
   (if (check-invoice-auth invoice-id (get-current-company-id) :verify)
     (set-invoice-status! invoice-id :verified)
     (throw status/forbidden))))

(defn reject-invoice! [invoice-id]
  (transaction
   (if (check-invoice-auth invoice-id (get-current-company-id) :verify)
     (set-invoice-status! invoice-id :rejected)
     (throw status/forbidden))))



(defn get-all-invoices-for-supplier [page order dir]
  (get-invoices-for-supplier nil (get-current-company-id) page 30 order dir))
(defn get-unverified-invoices-for-supplier [page order dir]
  (get-invoices-for-supplier ["unverified"] (get-current-company-id) page 30 order dir))

(defn get-all-invoices-for-buyer [page order dir]
  (get-invoices-for-buyer nil (get-current-company-id) page 30 order dir))
(defn get-unverified-invoices-for-buyer [page order dir]
  (get-invoices-for-buyer ["unverified"] (get-current-company-id) page 30 order dir))
