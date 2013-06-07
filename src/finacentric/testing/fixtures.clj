(ns finacentric.testing.fixtures
  (:use [lobos.core :only (defcommand migrate)])
  (:require [clojure.java.jdbc :as sql]
            [lobos.migration :as lm]
            [finacentric.models.db :as db]
            [clj-http.client :as http]))

(def ^:dynamic *cookies* nil)

(defn action-post
  [url args]
  (http/post (str "http://localhost:3000" url)  {:form-params args :cookies *cookies*}))

(defn form-post
  [url args]
  (let [result (http/post (str "http://localhost:3000" url)  {:form-params args :cookies *cookies*})]
    (assert (= (:status result) 302))
    result))


(defn url [& args]
  (apply str
         (interleave
          (repeat "/")
          (for [x args]
            (cond-> x
              (keyword? x)
              name)))))

;; actions:

(defn create-admin! [email passwd]
  (let [user-id (->
                 (db/create-user {:email email :admin true})
                 (get :id))]
    (db/set-user-pass user-id passwd)
    user-id))


(defn create-company! [name & [domain]]
  (form-post "/admin/companies/new" {:name name :domain domain}))
(defn add-supplier! [buyer-id seller-id]
  (action-post (url :admin :companies buyer-id :suppliers seller-id :pin) {}))

(defn create-user-for-company! [company-id data passwd]
  (form-post (url :admin :companies company-id :users :new) data)
  (let [user-id (->
                 (db/find-user (data :email))
                 (get :id))]
    
    (form-post (url :admin :users user-id :set-passwd) {:password passwd :repeat-password passwd})
    user-id))




(defn login [login-url user-id pass]
  (let [result (http/post login-url {:form-params {:id user-id :pass pass}})]
    (assert (= (:status result) 302))
    (:cookies result)))


(defmacro with-logged-user [[user-id pass] & body]
  `(binding [*cookies* (login "http://localhost:3000/login" ~user-id ~pass)]
     ~@body))




(def companies
  [["D-Tel" "dtel"]
   ["B-Tel" "btel"]
   ;; ["Sznurki"]
   ;; ["Kable"]

   ])

(def suppliers
  {1 [3 4]
   2 [3]})
  



(defn init-db []
  (lobos.core/reset)
  (create-admin! "admin" "a1234")
  (with-logged-user ["admin" "a1234"]
    (doseq [a companies] (apply create-company! a))
    (create-user-for-company! 1 {:email "a@dtel.pl" :first_name "Adam" :last_name "Kowalski"} "abcde"))
    ;(doseq [[k,v] suppliers v v] (add-supplier! k v))
  
  
  )
  
  
  

    
