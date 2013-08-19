(ns finacentric.model.companies
  (:use finacentric.schema
        [korma.core]; :only [insert values update set-fields where select]]
        [korma.db :only [transaction rollback]]
        finacentric.model.utils
        ))



(defn create-supplier! [buyer-id data]
  (transaction
   (let [company (insert COMPANIES (values (assoc data :is_buyer false)))]
     (insert SELLERS-BUYERS (values {:seller_id (company :id) :buyer_id buyer-id}))
     company)))

(defn create-buyer-company! [name]
  (insert COMPANIES (values {:name name :is_buyer true})))


(defn get-company-mode [company-id]
  (let [company (select COMPANIES (where {:id company-id}) (fields :is_buyer))]
    (prn :1 company company-id)
    (if (-> company
            first
            :is_buyer)
      :buyer
      :supplier)))

(defn get-company-data [id]
  (-> (select COMPANIES (where {:id id})) first))



(defn get-suppliers [company-id page-no per-page order-by dir]
  (select SELLERS-BUYERS
          (from COMPANIES)
          (where (and (= :buyer_id company-id)
                      (= :seller_id :companies.id)))
          (fields "companies.*")
          (page page-no per-page)
          ))





(defn get-buyers-for-company [company-id]
  (->> (select SELLERS-BUYERS
          (where {:seller_id company-id})
          (fields :buyer_id))
       (map :buyer_id)))
