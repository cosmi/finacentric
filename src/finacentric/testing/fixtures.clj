(ns finacentric.testing.fixtures
  (:use [lobos.core :only (defcommand migrate)])
  (:require [clojure.java.jdbc :as sql]
            [lobos.migration :as lm]
            [clj-http.client :as http]))

(def ^:dynamic *cookies* nil)

(defn form-post
  [url args]
  (http/post (str "http://localhost:3000" url)  {:form-params args :cookies *cookies*}))

(defn admin-post
  [url args]
  (form-post (str "/admin" url) args))


(defn url [& args]
  (apply str
         (interleave
          (repeat "/")
          (for [x args]
            (cond-> x
              (keyword? x)
              name)))))

;; actions:

(defn create-company! [name & [domain]]
  (admin-post "/companies/new" {:name name :domain domain}))
(defn add-supplier! [buyer-id seller-id]
  (admin-post (url :companies buyer-id :suppliers seller-id :pin) {}))




(defn login [login-url user-id pass]
  (let [result (http/post login-url {:form-params {:id user-id :pass pass}})]
    (when (= (:status result) 302)
      (:cookies result))))


(defmacro with-logged-user [[user-id pass] body]
  `(binding [*cookies* (login "http://localhost:3000/login" ~user-id ~pass)]
     ~@body))




(def companies
  [["D-Tel" "dtel"]
   ["B-Tel" "btel"]
   ["Sznurki"]
   ["Kable"]])

(def suppliers
  {1 [3 4]
   2 [3]})
  



(defn init-db []
  (lobos.core/reset)
  (doseq [a companies] (apply create-company! a))
  (doseq [[k,v] suppliers v v] (add-supplier! k v))


  )
  
  
  

    
