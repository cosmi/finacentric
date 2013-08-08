(ns finacentric.schema
  (:use korma.db
        korma.core))
  
(defdb db (postgres {:db "mydb"
                     :user "user"
                     :password "dbpass"}))




(defentity USERS)
