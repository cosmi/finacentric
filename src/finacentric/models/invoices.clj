(ns finacentric.models.invoices
  (:require [finacentric.models.db :as db])
  (:use [korma.core :as korma]
        [korma.db :only (defdb transaction)])
  (:require [clj-time.core :as time])
  (:import [org.joda.time Days LocalDate]))


;; Status faktury:
;; S.C. wprowadza
;; - input
;; B.C. potwierdza poprawność
;; - accepted
;; B.C. składa ofertę obniżki
;; - discount offer
;; S.C. zatwierdza ofertę i wybiera datę
;; - discount accepted
;; B.C. zgadza się na wybraną datę
;; - discount confirmed
;; S.C. potwierdza, że wystawi korektę i B.C. może robić przelew w ustalonym dniu
;; - correction done
;; B.C. opcjonalnie potwierdza otrzymanie korekty
;; - (correction received)

(defmacro throw-on-nil [& body]
  `(let [ret# (do ~@body)]
     (if-not ret#
       (throw (Exception. "Nil returned"))
       ret#)))

(def state-filters
  {:input {:accepted nil
           :rejected nil}
   :accepted {:accepted [not= nil]
              :annual_discount_rate nil
              :earliest_discount_date nil}
   :rejected {:rejected [not= nil]}
   :discount_offered {:annual_discount_rate [not= nil]
                    :earliest_discount_date [not= nil]
                    :discount_accepted nil}
   :discount_accepted {:discount_accepted [not= nil]
                       :discount_confirmed nil}
   :discount_confirmed {:discount_confirmed [not= nil]
                        :corrected nil}
   :correction_done {:corrected [not= nil]
                     :correction_received nil}
   :correction_received {:correction_received [not= nil]}
   })

(def state-checkers
  (into {}
        (for [[state, values] state-filters]
          [state
           (apply every-pred
                  (for [[k, v] values]
                    (if (vector? v)
                      (let [[f & args] v]
                        #(apply f (get % k) args))
                      #(= (get % k) v))))])))

(defn get-state [invoice]
  (->> state-checkers
       (filter (fn [[s,f]] (f invoice)))
       first
       first))

(defn append-state [invoice]
  (assoc invoice :state (get-state invoice)))

(defmacro at-state [query state]
  `(where ~query (state-filters ~state)))


(defn reachable? [party-id invoice-id]
  (->
   (select db/invoices
     (where (and (or (= party-id :buyer_id)
                     (= party-id :seller_id))
                 (= :id invoice-id)))
     (aggregate (count :*) :cnt))
   first :cnt
   (> 0)))

(defn has-state? [party-id invoice-id state]
  (->
   (select db/invoices
     (where (and (or (= party-id :buyer_id)
                     (= party-id :seller_id))
                 (= :id invoice-id)))
     (at-state state)
     (aggregate (count :*) :cnt))
   first :cnt
   (> 0)))
  
  

(defn invoice-input! [supplier-id buyer-id data]
  (db/simple-create-invoice data supplier-id buyer-id))

(defn invoice-accept! [company-id invoice-id]
  (throw-on-nil
    (update db/invoices
      (where {:buyer_id company-id
              :id invoice-id})
      (at-state :input)
      (set-fields {:accepted (sqlfn :now)}))))


(defn invoice-reject! [company-id invoice-id]
  (throw-on-nil
    (update db/invoices
      (where {:buyer_id company-id
              :id invoice-id})
      (at-state :input)
      (set-fields {:rejected (sqlfn :now)}))))

(defn invoice-accept-cancel! [company-id invoice-id]
  (throw-on-nil
    (update db/invoices
      (where {:buyer_id company-id
              :id invoice-id})
      (at-state :accepted)
      (set-fields {:accepted nil}))))

(defn invoice-reject-cancel! [company-id invoice-id]
  (throw-on-nil
    (update db/invoices
      (where {:buyer_id company-id
              :id invoice-id})
      (at-state :rejected)
      (set-fields {:rejected nil}))))

(defn invoice-offer-discount! [company-id invoice-id annual-rate earliest-date]
  (throw-on-nil
    (update db/invoices
      (where {:buyer_id company-id
              :id invoice-id})
      (at-state :accepted)
      (set-fields {:annual_discount_rate annual-rate
                   :earliest_discount_date earliest-date}
                   
                   ))))

(defn sql-date-to-joda [^java.sql.Date value]
  (org.joda.time.LocalDate/fromDateFields value)
  )

(defn calculate-discount-rate
  "Przy początkowej i końcowej dacie zwraca wyliczony dyskont;
UWAGA: na wejściu wartość wyrażona w procentach, na wyjściu również,
z dokładnością do 4 cyfr po przecinku"
  [annual-percent-rate first-date last-date]
  {:pre [(< annual-percent-rate 100.)]
   :post [(< % 100.)]}
   
  (let [first-date (sql-date-to-joda first-date)
        last-date (sql-date-to-joda last-date)
        _ (assert (-> first-date (.compareTo last-date) (<= 0)))
        days-diff (.getDays (org.joda.time.Days/daysBetween first-date last-date))
        date-diff (/ days-diff 365.)
    ;; można nie zakładać, że rok ma 365 dni, ale wygląda na to, że
    ;; na rynkach finansowych stosuje się takie uproszczenie przy krótszych niż rok
    ;; okresach - a tu okres nie będzie dłuższy niż 92 dni
        annual-rate (/ annual-percent-rate 100M)
        float-rate (- 1.0 (Math/pow (- 1 annual-rate) date-diff))]
    (-> float-rate (* 100.) bigdec (.setScale 4 java.math.BigDecimal/ROUND_HALF_UP))
    ))

(defn apply-discount [old-value discount-value]
  (* old-value (- 100M discount-value) 0.01M))

(defn get-discount-values [invoice-id new-payment-date]
  (let [invoice (->
                 (select db/invoices
                   (where {:id invoice-id})
                   (at-state :discount_offered)
                   first))
        discount-rate (calculate-discount-rate
                       (invoice :annual_discount_rate)
                       new-payment-date
                       (invoice :payment_date))
        new-net-value (apply-discount (invoice :net_total) discount-rate)
        new-gross-value (apply-discount (invoice :gross_total) discount-rate)]
    {:discounted_net_total new-net-value
     :discounted_gross_total new-gross-value
     :discount_rate discount-rate}))

(defn invoice-accept-discount! [supplier-id invoice-id new-payment-date]
    (transaction
      (let [values (get-discount-values invoice-id new-payment-date)]
        (update db/invoices
          (where {:seller_id supplier-id
                  :invoice_id invoice-id})
          (at-state :discount_offered)
          (set-fields values)))))

(defn invoice-confirm-discount! [company-id invoice-id net-value gross-value]
  )

(defn invoice-correction-done! [company-id invoice-id]
  )



(def not-rejected-filter
  #(where % {:rejected nil}))