(ns lobos.migrations
  (:refer-clojure
   :exclude [alter drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema config)))

(defmigration add-users-table
  (up [] (create
          (table :users
                 (varchar :id 20 :primary-key)
                 (varchar :first_name 30)
                 (varchar :last_name 30)
                 (varchar :email 30)
                 (boolean :admin)
                 (time    :last_login)
                 (boolean :is_active)
                 (varchar :pass 100))))
  (down [] (drop (table :users))))
