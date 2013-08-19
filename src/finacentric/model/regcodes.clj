(ns finacentric.model.regcodes
  (:require [noir.util.crypt :as crypt]
            [noir.session :as session])
  (:use finacentric.schema
        [korma.core]; :only [insert values update set-fields where select]]
        [korma.db :only [transaction rollback]]
        finacentric.model.utils
        ))


(defn get-company-regcode [regcode]
  (-> (select COMPANY-REGCODES (where {:regcode regcode :used false}) (limit 1)) first))


(defn use-up-regcode! [regcode-id company-id]
  (or 
   (update COMPANY-REGCODES (where {:id regcode-id})
          (set-fields {:target_company_id company-id :used true}))
   (throw (ex-info "Invalid regcode" {:regcode-id regcode-id}))))




(defn get-regcodes [company-id page-no per-page order-by dir]
  (select COMPANY-REGCODES
          (where {:owner_company_id company-id
                  :used false})
          (page page-no per-page)
          ))



(defn create-regcode! [company-id name email]
  (insert COMPANY-REGCODES
          (values {:name name :email email :owner_company_id company-id :regcode (generate-regcode)})))



(defn check-regcode-auth [regcode-id company-id action]
  (case action
    :delete 
     (= company-id
        (-> (select COMPANY-REGCODES (where {:id regcode-id}) (fields :owner_company_id)) first :owner_company_id))))


(defn delete-regcode! [regcode-id]
  (delete COMPANY-REGCODES (where {:id regcode-id})))
