(ns lobos.migrations
  (:refer-clojure
   :exclude [alter drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema config)))

(defmigration add-users-table
  (up [] (create
          (table :users
                 (integer :id :primary-key :auto-inc)
                 (varchar :first_name 30)
                 (varchar :last_name 30)
                 (varchar :email 40)
                 (boolean :admin)
                 (time    :last_login)
                 (boolean :is_active)
                 (varchar :pass 100))))
  (down [] (drop (table :users))))


(defmigration add-domain
  (up []
      (create
       (table :domains
              (integer :id :primary-key :auto-inc)
              (varchar :domain 30 :unique)
              (varchar :name 40 :unique)
              (boolean :is_active)))
      (alter :add
             (table :users 
                    (integer :domain-id [:refer :domains :id] :not-null)))
      (create
       (index :users :users-email-domain [:email :domain-id] :unique)))
  (down []
        (drop (index :users :users-email-domain))
        (alter :drop
               (table :users
                      (column :domain-id)))
        (drop (table :domains))))