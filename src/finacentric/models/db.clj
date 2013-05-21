(ns finacentric.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [finacentric.models.schema :as schema]))

(defdb db schema/db-spec)

(defn page [query page-no per-page]
  (-> query
      (offset (* page-no per-page))
      (limit per-page)))

(defmacro select-one [ent & body]
  `(first (select ~ent (limit 1) ~@body)))

(declare companies users company_datas invoices invoices-send invoices-recv invoice_lines)

(defentity users
  (belongs-to companies))

(defentity buyers
  (table :suppliers :buyers)
  (belongs-to companies {:fk :buyer_id}))

(defentity sellers
  (table :suppliers :sellers)
  (belongs-to companies {:fk :seller_id}))

(defentity companies
  (has-many users)
  (belongs-to company_datas {:fk :data_id}) ;; Single current company data
  (has-many buyers {:fk :seller_id})
  (has-many sellers {:fk :buyer_id})
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


(defn create-company [data]
  (insert companies
    (values data)))

(defn update-company [id data]
  (update companies
    (where {:id id})
    (set-fields data)))
