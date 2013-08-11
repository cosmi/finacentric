(ns finacentric.model.users
  (:require [noir.util.crypt :as crypt]
            [noir.session :as session])
  (:use finacentric.schema
        [korma.core]; :only [insert values update set-fields where select]]
        ))



(defn get-login-data-by-email [email]
  (-> (select USERS (where {:email email})) first))

(defn get-login-data [email password]
  (when-let [user-data (get-login-data-by-email email)]
    (when (crypt/compare password (user-data :pass))
      (-> (select-keys user-data [:email :id])
          (assoc :name (if (or (user-data :first_name) (user-data :last_name))
                         (format "%s %s" (user-data :first_name) (user-data :last_name))
                         (user-data :email)))))))

(defn create-user! [user-data]
  (insert USERS (values user-data)))

(defn- encrypt-pass [pass]
  (crypt/encrypt pass))

(defn set-user-pass! [user-id pass]
  (let [encrypted (encrypt-pass pass)]
    (or (update USERS
                (where (= :id user-id))
                (set-fields {:pass encrypted}))
        (throw (ex-info "Could not change password" {:user-id user-id})))))

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
  (or
   (update USERS (where {:id user-id}) (set-fields {:company_id company-id}))
   (throw (ex-info "No such user" {:user-id user-id}))))
