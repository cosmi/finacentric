(ns finacentric.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [finacentric.models.schema :as schema]))

(defdb db schema/db-spec)

(declare companies users suppliers company_datas invoices invoice_lines)

(defentity users
  (belongs-to companies))

(defentity companies
  (has-many users)
  (has-many company_datas)
  (belongs-to company_datas {:fk :data_id}) ;; Single current company data
  (many-to-many companies :suppliers {:lfk :seller_id :rfk :buyer_id})
  (has-many invoices {:fk :seller_id})
  (has-many invoices {:fk :buyer_id}))

(defentity suppliers
  (belongs-to companies {:fk :seller_id})
  (belongs-to companies {:fk :buyer_id}))

(defentity company_datas
  (belongs-to companies)
  (has-many invoices {:fk :seller_data_id})
  (has-many invoices {:fk :buyer_data_id}))

(defentity invoices
  (belongs-to companies {:fk :seller_id})
  (belongs-to companies {:fk :buyer_id})
  (belongs-to company_datas {:fk :seller_data_id})
  (belongs-to company_datas {:fk :buyer_data_id})
  (has-many invoice_lines))

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
