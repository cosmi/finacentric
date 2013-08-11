(ns finacentric.model.companies
  (:require [noir.util.crypt :as crypt]
            [noir.session :as session])
  (:use finacentric.schema
        [korma.core]; :only [insert values update set-fields where select]]
        [korma.db :only [transaction rollback]]
        ))

(defn get-company-regcode [regcode]
  (-> (select COMPANY-REGCODES (where {:regcode regcode :used false}) (limit 1)) first))


(defn create-supplier! [buyer-id data]
  (transaction
   (let [company (insert COMPANIES (values (assoc data :is_buyer false)))]
     (insert SELLERS-BUYERS (values {:seller_id (company :id) :buyer_id buyer-id})))))

(defn use-up-regcode! [regcode-id company-id]
  (or 
   (update COMPANY-REGCODES (where {:id regcode-id})
          (set-fields {:target_company_id company-id :used true}))
   (throw (ex-info "Invalid regcode" {:regcode-id regcode-id}))))

(defn create-buyer-company! [name]
  (insert COMPANIES (values {:name name :is_buyer true})))


(defn get-company-mode [company-id]
  (let [company (select COMPANIES (where {:id company-id}) (fields :is_buyer))]
    (prn :1 company company-id)
    (if (-> company
            first
            :is_buyer)
      :buyer
      :supplier)))


