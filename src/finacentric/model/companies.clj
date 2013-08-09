(ns finacentric.model.companies
  (:require [noir.util.crypt :as crypt]
            [noir.session :as session])
  (:use finacentric.schema
        [korma.core]; :only [insert values update set-fields where select]]
        ))

(defn get-company-with-regcode [regcode]
  (-> (select COMPANIES (where {:regcode regcode}) (limit 1)) first))

(defn finalize-company! [company-id name]
  (update COMPANIES (where {:company_id company-id :regcode (not= nil)}) (set-fields {:regcode nil :name name})))

(defn create-buyer-company! [name]
  (insert COMPANIES (values {:name name})))
