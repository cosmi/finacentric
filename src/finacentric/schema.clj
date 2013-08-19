(ns finacentric.schema
  (:use korma.db
        korma.core
        causeway.bootconfig))


(defdb db (bootconfig :db-spec))




(defentity USERS (table :users))
(defentity COMPANIES (table :companies))
(defentity COMPANY-REGCODES (table :company_regcodes))
(defentity SELLERS-BUYERS (table :sellers_buyers))

(defentity INVOICES (table :invoices))
