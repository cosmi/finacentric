(ns finacentric.model.invoices
  (:use finacentric.schema
        [korma.core]; :only [insert values update set-fields where select]]
        [korma.db :only [transaction rollback]]
        finacentric.model.utils)
  (:require [finacentric.utils.upload :as upload]))


(defn check-invoice-auth [invoice-id company-id action]
  (case action
    :delete
    (-> (select INVOICES (where {:id invoice-id :seller_id company-id :state "unverified"})) boolean)
    :delete-file
    (-> (select INVOICES (where {:id invoice-id :seller_id company-id :state "unverified"})) boolean)
    :verify
    (-> (select INVOICES (where {:id invoice-id :buyer_id company-id :state "unverified"})) boolean)
  ))



(defn create-invoice! [supplier-id buyer-id data]
  (insert INVOICES
          (values {:seller_id supplier-id :buyer_id buyer-id
                   :number (data :number)
                   :issue_date (data :issue_date)
                   :payment_date (data :payment_date)
                   :net_total (data :net_total)
                   :gross_total (data :gross_total)})))


(defn upload-invoice-file! [invoice-id file]
  (let [file-id (when file
                  (upload/store-file-input! file {}))]
    (or (update INVOICES (where {:id invoice-id}) (set-fields {:file_id file-id}))
        (throw (Exception. "Cannot update"))
        )))

(defn delete-invoice-file! [invoice-id]
  (or 
   (update INVOICES (where {:id invoice-id}) (set-fields {:file_id nil}))
   (throw (Exception. "Cannot update"))))

(defn delete-invoice! [invoice-id]
  (or 
   (delete INVOICES (where {:id invoice-id}))
   (throw (Exception. "Cannot delete"))))

(defn set-invoice-status! [invoice-id status]
  (or (update INVOICES (where {:id invoice-id}) (set-fields {:state (name status)}))
      (throw (Exception. "Cannot update"))))



(defn get-invoices-for-supplier [states company-id page-no per-page order dir]
  (->
   (cond->
    (-> (select* INVOICES)
        (where {:seller_id company-id})
        (page page-no per-page))
    (-> states empty? not)
    (where {:state [in states]}))
   exec))
    


(defn get-invoices-for-buyer [states company-id page-no per-page order dir]
  (let [states  (mapv name states)]
    (->
     (cond->
      (-> (select* INVOICES)
          (where {:buyer_id company-id})
          (page page-no per-page))
      (-> states empty? not)
      (where {:state [in states]}))
     exec)))
