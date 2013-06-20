(ns finacentric.models.db
  (:use [korma.core :as korma]
        [korma.db :only (defdb transaction)])
  (:require [finacentric.models.schema :as schema]
            [finacentric.mail :as mail]
            [noir.util.crypt :as crypt]))

(defdb db schema/db-spec)

(defn page [query page-no per-page]
  (-> query
      (offset (* page-no per-page))
      (limit per-page)))


(defmacro select-one [ent & body]
  `(first (select ~ent (limit 1) ~@body)))


(defn- to-sql-date [date]
  (java.sql.Date. date))

(declare companies users company_datas invoices invoices-send invoices-recv invoice_lines)

(defentity users
  (belongs-to companies))

(defentity users
  (belongs-to companies))

(defentity admins
  (table (subselect users
            (where {:admin true}))
         :admins))

(defentity buyers
  (table :sellers_buyers :buyers)
  (belongs-to companies {:fk :buyer_id}))

(defentity sellers
  (table :sellers_buyers :sellers)
  (belongs-to companies {:fk :seller_id}))



(defentity companies
  (has-many users)
  (belongs-to company_datas {:fk :data_id}) ;; Single current company data
  (has-many buyers {:fk :seller_id})
  (has-many sellers {:fk :buyer_id})
  (has-many invoices-send {:fk :seller_id})
  (has-many invoices-recv {:fk :buyer_id}))

;; (defentity suppliers
;;   (table (subselect sellers
;;                     (fields "companies.*" :buyer_id)
;;                     (join :right companies
;;                           (= :companies.id :seller_id))) :suppliers))






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
  (has-many invoice_lines)
  ;; (prepare (fn [v]
  ;;            (reduce #(if-let [d (%1 %2)]
  ;;                       (assoc %1 %2 (to-sql-date d))
  ;;                       %1) v [:issue_date :sell_date :payment_date :discounted_payment_date])))
  )

;; (defentity invoices-send
;;   (table :invoices :invoices-send))

;; (defentity invoices-recv
;;   (table :invoices :invoices-recv))

(defentity invoice_lines
  (belongs-to invoices))

(defn create-user! [user-data]
  (insert users (values user-data)))

(defn encrypt-pass [pass]
  (crypt/encrypt pass))

(defn set-user-pass [user-id pass]
  (let [encrypted (encrypt-pass pass)]
    (update users
      (where (= :id user-id))
      (set-fields {:pass encrypted}))))

(defn update-user [id first-name last-name email]
  ;;   (update users
  ;;           (set-fields {:first_name first-name
  ;;                        :last_name last-name
  ;;                        :email email})
  ;;           (where {:id id}))
  )

(defn get-user [id]
    (first (select users
                   (where {:id id})
                   (limit 1))))
(defn find-user [login]
    (first (select users
                   (where {:email login})
                   (limit 1))))

(defn is-admin? [user-id]
  (-> (select users
        (fields :id :admin)
        (where {:id user-id})
        (limit 1))
      first
      (get :admin)))


(defn get-users-company-id [user-id]
  (-> (select users
        (where {:id user-id})
        (fields :company_id)
        (limit 1))
      first
      (get :company_id)))


(defn get-suppliers-first-buyer-id [supplier-id]
  (-> (select sellers
        (where {:seller_id supplier-id})
        (fields :buyer_id)
        (limit 1))
      first
      (get :buyer_id)))




(defn user-to-company-access? [user-id company-id]
  (-> (select users
        (fields :id :company_id)
        (where {:id user-id})
        (limit 1))
      first
      (get :company_id)
      (= company-id)))


(defn users-pin-to-company [user-id company-id]
  (korma/update users (korma/where {:id user-id})
                (korma/set-fields {:company_id company-id}))
  )
(defn users-set-admin-state [user-id state]
  (korma/update users (korma/where {:id user-id})
                (korma/set-fields {:admin state})))

(defn create-company-for-user [user-id data]
  (transaction
    (let [company-id (-> (insert companies (values data)) :id)]
      (update users (korma/where {:id user-id})
              (set-fields {:company_id company-id})))))


(defn update-company [id data]
    (update companies
      (where {:id id})
      (set-fields data)))

(defn get-company [id]
  (-> (select companies
        (where {:id id})
        (limit 1))
      first))


(defn create-supplier! [buyer-id seller-id]
  (insert sellers (values {:buyer_id buyer-id :seller_id seller-id}))
  )

(defn delete-supplier! [buyer-id seller-id]
  (delete sellers (where {:buyer_id buyer-id :seller_id seller-id})))


(defn simple-create-invoice [obj seller-id buyer-id]
  (transaction
    (let [data-ids (group-by :id
                             (select companies
                               (where (or (= :id seller-id) (= :id buyer-id)))
                               (fields [:id :data_id])))
          data-ids #(-> data-ids (get %) (get :data_id))]
      
      (insert invoices (values (merge obj
                                      {:seller_id seller-id :buyer_id buyer-id
                                           :seller_data_id (data-ids seller-id)
                                           :buyer_data_id (data-ids buyer-id)
                                           }))))))





(defn create-supplier-for-company! [company-id data invite-emails]
  (let [reg-token (crypt/gen-salt) ;;TODO coś łądniejszego
        id (transaction
             (let [data (insert company_datas (values data))
                   supplier (insert companies (values {:name (data :name)
                                                       :reg_token reg-token
                                                       :data_id (data :id)}))]
               (create-supplier! company-id (supplier :id))
               (supplier :id)))]
    (doseq [email invite-emails]
      (mail/send-reg-token! email reg-token))
    id
    ))

(defn get-company-with-reg-code [reg-code]
  (-> (select companies (where (= :reg_token reg-code)) (limit 1))
      first))

(defn create-user-from-reg-code! [reg-code user-data password]
  (or
   (transaction
     (when-let [company (get-company-with-reg-code reg-code)]
       (let [user (create-user! (assoc user-data :company_id (company :id) :pass (encrypt-pass password)))]
         (update companies
           (where {:id (company :id)})
           (set-fields {:reg_token nil}))
         (user :id))))
   (throw (ex-info "No such code" {:reg-code reg-code}))))




;; NOWE ZAJEBISTE ZAPYTANIAs;


(defn page-filter [page-no per-page]
  #(page % page-no per-page))

(defn sorted-by [field & [dir]]
  #(order % field dir))



(defn get-invoices [from to & filters]
  (->
   (reduce #(%2 %1)
           (-> (select* invoices)
               (where {:seller_id from :buyer_id to}))
           filters)
   exec))



(defn get-invoices-with-suppliers [buyer-id & filters]
  (->
   (reduce #(%2 %1)
           (-> (select* invoices)
               (where {:buyer_id buyer-id})
               (join :left
                     companies
                     (= :invoices.seller_id :companies.id)
                     )
               (fields "invoices.*" :companies.name))
           filters)
   exec))


(defn get-invoice [invoice-id supplier-id buyer-id]
  (->
   (select invoices
     (where {:seller_id supplier-id
             :buyer_id buyer-id
             :id invoice-id})
     (limit 1))
   first))

(defn get-invoice-unchecked [invoice-id]
  (->
   (select invoices
     (where {:id invoice-id})
     (limit 1))
   first))

(defn fetch-potential-suppliers [buyer-id & filters]
  (->
   (reduce #(%2 %1)
           (-> (select* companies)
               (join :left
                     sellers
                     (and
                      (= :sellers.seller_id :companies.id)
                      (= :sellers.buyer_id buyer-id)))
               (where (not= buyer-id :companies.id))
               (fields "companies.*" :sellers.buyer_id)
               )
           filters)
   exec))
