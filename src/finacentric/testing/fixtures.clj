(ns finacentric.testing.fixtures
  (:use [lobos.core :only (defcommand migrate)])
  (:use [korma.core :as korma]
        [clojure.pprint]
        [korma.db :only (defdb transaction)])
  (:require [clojure.java.jdbc :as sql]
            [net.cgrand.enlive-html :as enlive]
            [lobos.migration :as lm]
            [finacentric.models.db :as db]
            [clj-http.client :as http]
            [clojure.string :as strings])
  (:import [java.io.StringReader]))

(def ^:dynamic *cookies* nil)

(defn action-post
  [url args]
  (http/post (str "http://localhost:3000" url)  {:form-params args :cookies *cookies*}))

(defn form-post
  [url args]
  (let [result (http/get (str "http://localhost:3000" url)  { :cookies *cookies*})
        body (-> result :body (java.io.StringReader.) enlive/html-resource)]
    (assert (= (:status result) 200))
    (let [forms (->> (enlive/select body [:form])
                     (filter (fn [form]
                               (and (-> form :attrs :method strings/lower-case (= "post"))
                                    (-> form :attrs :action nil?)))))]
      (assert (-> forms count (= 1)) "More than one form!")
      (let [form (first forms)
            unknown-fields (->> args
                                keys
                                (remove #(->
                                          (enlive/select form
                                                         [[#{:input :select :textarea}
                                                           (enlive/attr= :name (name %))]])
                                          not-empty)))]
        (assert (empty? unknown-fields)
                (apply str "Fields do not exist in the form: "
                       (interpose ", " unknown-fields))))))
  (let [result (http/post (str "http://localhost:3000" url)
                          {:form-params args :cookies *cookies*})]
    (assert (= (:status result) 302) (str "Error, status returned: " (:status result)))
    result))


(defn url [& args]
  (apply str
         (interleave
          (repeat "/")
          (for [x args]
            (cond-> x
                    (keyword? x)
                    name)))))



(defn login [login-url user-id pass]
  (let [result (http/post login-url {:form-params {:id user-id :pass pass}})]
    (assert (= (:status result) 302) (str "Error, status returned: " (:status result)))
    (:cookies result)))

(defn user-company-id [email]
  (->
   (select db/users (where (= :email email)) (fields :company_id))
   first
   :company_id))

(defmacro with-logged-user [[user-id pass] & body]
  `(binding [*cookies* (login "http://localhost:3000/login" ~user-id ~pass)]
     ~@body))


;; admin-actions:

(defn create-admin! [email passwd]
  (let [user-id (->
                 (db/create-user! {:email email :admin true})
                 (get :id))]
    (db/set-user-pass user-id passwd)
    user-id))


(defn create-company! [name & [domain]]
  (form-post "/admin/companies/new" {:name name :domain domain})
  (-> (select db/companies (where {:name name :domain domain}) (order :id :DESC) (limit 1))
      first
      (get :id))
  )
(defn add-supplier! [buyer-id seller-id]
  (action-post (url :admin :companies buyer-id :suppliers seller-id :pin) {}))

(defn create-user-for-company! [company-id data passwd]
  (form-post (url :admin :companies company-id :users :new) data)
  (let [user-id (->
                 (db/find-user (data :email))
                 (get :id))]

    (form-post (url :admin :users user-id :set-passwd) {:password passwd :repeat-password passwd})
    user-id))

;; non-admin-actions:

(defn create-supplier! [buyer-id data]
  (let [res (form-post (url :company :add-supplier)
                       data)]
    (-> res (get-in [:headers "location"])
        (->> (re-matches #"supplier/([0-9]+)"))
        second
        (Integer/parseInt)) ;; TODO? teraz zakłada,że zostanie przekierowany na supplier/company-id
    ))

(defn register-user-for-company-by-reg-code! [company-id user-data]
  (let [reg-token (-> (korma/select db/companies
                                    (where {:id company-id})
                                    (limit 1)
                                    (fields :reg_token))
                      first
                      (get :reg_token))]
    (form-post (url :supplier :register) (assoc user-data :reg-code reg-token))
    (-> (korma/select db/users
                      (where {:email (user-data :email)}))
        first
        (get :id))))


(defn create-simple-invoice! [data]
  (form-post (url :supplier :simple-invoice-form) data))



(defn accept-invoice! [id]
  (action-post (url :company :invoice id :accept) {}))

(defn reject-invoice! [id]
  (action-post (url :company :invoice id :reject) {}))

(defn offer-discount! [id date rate]
  (form-post (url :company :invoice id :offer-discount) {:earliest_discount_date date :annual_discount_rate rate}))


(defn accept-discount! [id date]
  (let [result (http/get (str "http://localhost:3000" (url :supplier :invoice id :discount-accept))  { :cookies *cookies*})
        body (-> result :body (java.io.StringReader.) enlive/html-resource)]
  
    (assert (= (:status result) 200))
    (let [forms (->> (enlive/select body [:form])
                     (filter (fn [form]
                               (and (-> form :attrs :method strings/lower-case (= "post"))
                                    (-> form :attrs :action nil?)))))
          form (first forms)
          values (enlive/select form
                                [[#{[:input (enlive/attr= :type "hidden")]
                                    [:input (enlive/attr= :type "text")]
                                    [:input (enlive/attr= :type nil)]
                                    }]])

          values (into {} (for [tag values]
                            [(-> tag :attrs :name name keyword)
                             (-> tag :attrs :value)]))]
      (form-post (url :supplier :invoice id :discount-accept)
                 (merge {:discounted_payment_date date}
                        (select-keys values
                                    [:annual_discount_rate
                                     :earliest_discount_date
                                     :payment_date])
                        )))))



(defn accept-payment! [id date]
  (form-post (url :company :invoice id :confirm-discount (str "?date=" date)) data)
  )

(defn correction-done! [id data]
  (form-post (url :company :invoice id :correction) data)
  )

(defn init-db []
  (lobos.core/reset)
  (create-admin! "admin" "a1234")
  (let [company-id
        (with-logged-user ["admin" "a1234"]
          (let [company-id (create-company! "D-Tel" "dtel")]
            (create-user-for-company! company-id {:email "a@dtel.pl" :first_name "Adam" :last_name "Kowalski"} "abcde")
            company-id))]



    (doseq [supp [{:name "Druty Sp. z o.o." :email "adam@druty.pl"}
                  {:name "Kable Sp. z o.o." :email "adam@kable.pl"}]]
      (with-logged-user ["a@dtel.pl" "abcde"]
        (let [supplier-id (create-supplier! company-id {:name (supp :name)})]
          (register-user-for-company-by-reg-code! supplier-id
                                                  {:email (supp :email)
                                                   :password "abcde"
                                                   :repeat-password "abcde"}))))


    (with-logged-user ["adam@druty.pl" "abcde"]
      (doseq [data [
                    {:number "KR/4/1"
                     :issue_date "2013-04-01"
                     :sell_date "2013-04-01"
                     :payment_date "2013-04-15"
                     :net_total 100
                     :gross_total 123}
                    {:number "KR/4/2"
                     :issue_date "2013-04-01"
                     :sell_date "2013-04-01"
                     :payment_date "2013-04-15"
                     :net_total 1100
                     :gross_total 1123}
                    {:number "KR/5/1"
                     :issue_date "2013-05-03"
                     :sell_date "2013-05-03"
                     :payment_date "2013-05-23"
                     :net_total 2000
                     :gross_total 2460}
                    {:number "KR/5/2"
                     :issue_date "2013-05-13"
                     :sell_date "2013-05-13"
                     :payment_date "2013-05-30"
                     :net_total 1500
                     :gross_total 1845}
                    ]]
        (create-simple-invoice! data))
      )

    (with-logged-user ["adam@kable.pl" "abcde"]
      (doseq [data [
                    {:number "FV/4/1"
                     :issue_date "2013-04-01"
                     :sell_date "2013-04-01"
                     :payment_date "2013-04-15"
                     :net_total 100
                     :gross_total 123}
                    {:number "FV/4/2"
                     :issue_date "2013-04-01"
                     :sell_date "2013-04-01"
                     :payment_date "2013-04-15"
                     :net_total 1100
                     :gross_total 1123}
                    {:number "FV/5/1"
                     :issue_date "2013-05-03"
                     :sell_date "2013-05-03"
                     :payment_date "2013-05-23"
                     :net_total 2000
                     :gross_total 2460}
                    {:number "FV/5/2"
                     :issue_date "2013-05-13"
                     :sell_date "2013-05-13"
                     :payment_date "2013-05-30"
                     :net_total 1500
                     :gross_total 1845}
                     {:number "FV/5/3"
                     :issue_date "2013-05-13"
                     :sell_date "2013-05-13"
                     :payment_date "2013-05-30"
                     :net_total 1500
                     :gross_total 1845}
                      {:number "FV/5/4"
                     :issue_date "2013-05-13"
                     :sell_date "2013-05-13"
                     :payment_date "2013-05-30"
                     :net_total 1500
                     :gross_total 1845}
                      {:number "FV/5/5"
                     :issue_date "2013-05-13"
                     :sell_date "2013-05-13"
                     :payment_date "2013-05-30"
                     :net_total 1500
                     :gross_total 1845}
                    ]]
        (create-simple-invoice! data))


      )
    (with-logged-user ["a@dtel.pl" "abcde"]
      (accept-invoice! 3)
      (accept-invoice! 5)
      (reject-invoice! 2)
      (offer-discount! 3 "2013-05-15" 10.5)
      (offer-discount! 3 "2013-05-15" 11.5))

    (with-logged-user ["adam@druty.pl" "abcde"]
      (accept-discount! 3 "2013-05-16"))
    
    (with-logged-user ["a@dtel.pl" "abcde"]
      (accept-payment! 3 "2013-05-16"))

    
    (with-logged-user ["adam@druty.pl" "abcde"]
      (correction-done! 3 {:number "CV/5/5"
                           :issue_date "2013-05-13"
                           :payment_date "2013-05-30"
                           :net_total 1500
                           :gross_total 1845})
      )
    ))
