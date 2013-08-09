(ns finacentric.schema
  (:use korma.db
        korma.core
        causeway.bootconfig))


(defdb db (bootconfig :db-spec))




(defentity USERS (table :users))
(defentity COMPANIES (table :companies))
