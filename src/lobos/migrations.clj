(ns lobos.migrations
  (:refer-clojure
   :exclude [alter drop bigint boolean char double float time])
  (:use (lobos [migration :only [defmigration]] core schema config)))

(defmacro try-hard [& body]
  `(do ~@(for [l body] `(try ~l (catch Exception e#))))
  )
(defmigration init-tables
  (up [] 
      (create
       (table :companies
              (integer :id :primary-key :auto-inc)
              (varchar :name 50)
              (varchar :domain 30 :unique)
              (varchar :reg_token 50 :unique)
              ))
      
      (create
       (table :users
              (integer :id :primary-key :auto-inc)
              (varchar :first_name 30)
              (varchar :last_name 40)
              (varchar :email 50 :not-null :unique)
              (boolean :admin)
              (time    :last_login)
              (boolean :is_active (default true))
              (varchar :pass 60)
              (integer :company_id [:refer :companies :id])
              ))
      
      (create
       (table :sellers_buyers
              (integer :seller_id [:refer :companies :id])
              (integer :buyer_id [:refer :companies :id])
              (check "not_same" (!= :seller_id  :buyer_id))
              (primary-key [:seller_id :buyer_id])))
      
      (create
       (table :company_datas
              (integer :id :primary-key :auto-inc)
              (integer :company_id [:refer :companies :id])
              (varchar :name 30)
              (varchar :nip 10)
              (varchar :regon 14)

              (varchar :address_street 80)
              (varchar :address_street_no 20)
              (varchar :address_zipcode 6)
              (varchar :address_city 60)
              (varchar :bank_data 100)))
      
      (alter :add
             (table :companies 
                    (integer :data_id [:refer :company_datas :id])))
      
      (create
       (table :invoices
              (integer :id :primary-key :auto-inc)
              (integer :seller_id [:refer :companies :id])
              (integer :buyer_id [:refer :companies :id])
              (foreign-key [:buyer_id :seller_id] :sellers_buyers)
              (integer :seller_data_id [:refer :company_datas :id])
              (integer :buyer_data_id [:refer :company_datas :id])
              (varchar :number 30)
              (varchar :description 30)
              (unique [:number :buyer_id])
              (date :issue_date)
              (date :sell_date)
              (date :payment_date)
              (varchar :payment_mode 30)
              (varchar :state 30)

              (decimal :paid_already 15 2)

              
              (decimal :net_total 15 2)
              (decimal :gross_total 15 2)

              (varchar :currency 8 (default "PLN"))
              (varchar :extra 500)

              (decimal :annual_discount_rate 7 4) ; wymagana zniżka za przyspieszenie
              (date :earliest_discount_date) ; najwcześniejsza możliwa opcja zniżki
              
              (date :discounted_payment_date)
              (decimal :discount_rate 7 4)
              
              (decimal :discounted_net_total 15 2)
              (decimal :discounted_gross_total 15 2)
              
              (timestamp :accepted)
              (timestamp :rejected)
              (timestamp :discount_accepted)
              (timestamp :discount_confirmed)
              (timestamp :corrected)
              (timestamp :correction_received)

              (varchar :file_id 32)
              
              ))

      (create
       (table :corrections
              (integer :id :primary-key :auto-inc)
              (integer :invoice_id [:refer :invoices :id] :unique)
              (varchar :number 30)
              (date :issue_date)
              (date :payment_date)
              (decimal :net_total 15 2)
              (decimal :gross_total 15 2)
              (varchar :currency 8 (default "PLN"))
              (varchar :file_id 32)
              ))

      ;; (create
      ;;  (table :invoice_events
      ;;         (integer :id :primary-key :auto-inc)
      ;;         (integer :user_id [:refer :users :id])
      ;;         (varchar :event 30)
      ;;         (varchar :msg 300)
      ;;         (timestamp :ts)
      ;;         (integer :invoice_id [:refer :invoices :id] :unique)))
              

      ;; (create 
      ;;  (table :tax_rates
      ;;         (integer :id :primary-key :auto-inc)
      ;;         (date :start_date)
      ;;         (date :end_date)
      ;;         (varchar :name 30)
      ;;         (decimal :rate 7 2)))
      
      ;; (create
      ;;  (table :invoice_totals
      ;;         (integer :rate_id [:refer :tax_rates :id])
      ;;         (integer :invoice_id [:refer :invoices :id])
      ;;         (unique [:rate_id :invoice_id])
      ;;         (decimal :net_total 15 2)
      ;;         (decimal :gross_total 15 2)
      ;;         (decimal :discount_rate 7 4)))

      
      ;; (create
      ;;  (table :invoice_lines
      ;;         (integer :invoice_id [:refer :invoices :id])
      ;;         (integer :position)
      ;;         (primary-key [:invoice_id :position])
      ;;         (varchar :name 60)
      ;;         (decimal :amount 17 4)
      ;;         (varchar :unit 10)
      ;;         (decimal :net_unit_price 15 2)
      ;;         (decimal :net_price 15 2)
      ;;         (decimal :gross_unit_price 15 2)
      ;;         (decimal :gross_price 15 2)
      ;;         (decimal :vat 5 2)
      ;;         (varchar :extra 500)))

      )
  (down []
        (try-hard
         (drop (table :users) :cascade)
         (drop (table :invoices) :cascade)
         (drop (table :corrections) :cascade)
         ;; (drop (table :invoice_lines) :cascade)
         ;; (drop (table :invoice_events) :cascade)
         (drop (table :companies) :cascade)
         (drop (table :company_datas) :cascade)
         (drop (table :sellers_buyers) :cascade))))
