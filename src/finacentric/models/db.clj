(ns finacentric.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [finacentric.models.schema :as schema]))

(defdb db schema/db-spec)

(declare users domains)


(defentity users
  (belongs-to domains))

(defn create-user [user]
  (insert users
          (values user)))



(defn update-user [id first-name last-name email]
  (update users
          (set-fields {:first_name first-name
                       :last_name last-name
                       :email email})
          (where {:id id})))

(defn get-user [id]
  (first (select users
                 (where {:id id})
                 (limit 1))))


(defentity domains
  (entity-fields :id :name :domain :is_active)
  (has-many users))

(defn create-domain [domain]
  (insert domains
    (values domain)))