(ns finacentric.model.users
  (:require [noir.util.crypt :as crypt]
            [noir.session :as session])
  (:use finacentric.schema
        [korma.core]; :only [insert values update set-fields where select]]
        ))



(defn get-login-data-by-email [email password])

(defn get-login-data-by-username [username password])

(defn get-login-data [uname-or-email password]
  (some #(% uname-or-email password) [get-login-data-by-username get-login-data-by-email]))

(defn create-user! [user-data]
  (insert USERS (values user-data)))

(defn- encrypt-pass [pass]
  (crypt/encrypt pass))

(defn set-user-pass! [user-id pass]
  (let [encrypted (encrypt-pass pass)]
    (update USERS
      (where (= :id user-id))
      (set-fields {:pass encrypted}))))

(defn create-user-with-pass! [user-data pass]
  (let [encrypted (encrypt-pass pass)]
    (insert USERS (values (assoc user-data :pass encrypted)))))


(defn get-users-company-id [user-id]
  (-> (select USERS
              (where {:id user-id})
              (fields :company_id))
      first
      :company_id))


(defn set-users-company-id! [user-id company-id]
  (update USERS (where {:id user-id}) (set-fields {:company_id company-id})))
