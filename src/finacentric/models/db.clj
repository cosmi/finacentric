(ns finacentric.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [finacentric.models.schema :as schema]))

(defdb db schema/db-spec)

(declare companies users company_datas invoices invoices-send invoices-recv invoice_lines)

(defentity users
  (belongs-to companies))

(defentity companies
  (has-many users)
  (belongs-to company_datas {:fk :data_id}) ;; Single current company data
  (many-to-many companies :suppliers {:lfk :seller_id :rfk :buyer_id})
  (has-many invoices-send {:fk :seller_id})
  (has-many invoices-recv {:fk :buyer_id}))

(defentity company_datas)

(defentity seller_company
  (table :companies :seller_company))

(defentity buyer_company
  (table :companies :buyer_company))

(defentity seller_data
  (table :company_datas :seller_data))

(defentity buyer_data
  (table :company_datas :buyer_data))

(defentity invoices
  (belongs-to seller_company {:fk :seller_id})
  (belongs-to buyer_company {:fk :buyer_id})
  (belongs-to seller_data {:fk :seller_data_id})
  (belongs-to buyer_data {:fk :buyer_data_id})
  (has-many invoice_lines))

(defentity invoices-send
  (table :invoices :invoices-send))

(defentity invoices-recv
  (table :invoices :invoices-recv))

(defentity invoice_lines
  (belongs-to invoices))

(defn create-user [user]
  ;; (insert users
  ;;         (values user))
  )

(defn update-user [id first-name last-name email]
  ;;   (update users
  ;;           (set-fields {:first_name first-name
  ;;                        :last_name last-name
  ;;                        :email email})
  ;;           (where {:id id}))
  )

(defn get-user [id]
  ;;   (first (select users
  ;;                  (where {:id id})
  ;;                  (limit 1)))
  )


(defentity domains
  (entity-fields :id :name :domain :is_active)
  (has-many users))

(defn create-domain [domain]
  (insert domains
    (values domain)))
