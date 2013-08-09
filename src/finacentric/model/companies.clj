(ns finacentric.model.users
  (:require [noir.util.crypt :as crypt]
            [noir.session :as session])
  (:use finacentric.schema
        [korma.core]; :only [insert values update set-fields where select]]
        ))

(defn get-company-with-regcode [regcode]
  (-> (select COMPANIES (where {:regcode regcode}) (limit 1)) first))
