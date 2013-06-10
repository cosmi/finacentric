(ns finacentric.testing.fixtures
  (:use [lobos.core :only (defcommand migrate)])
  (:use [korma.core :as korma]
        [clojure.pprint]
        [korma.db :only (defdb transaction)])
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
    (pprint result)
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
  (let [res (form-post (url :company buyer-id :add-supplier)
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
    (form-post (url :supplier :register) (assoc user-data :reg-token reg-token))
    (-> (korma/select db/users
          (where {:email (user-data :email)}))
        first
        (get :id))))

  



(defn init-db []
  (lobos.core/reset)
  (create-admin! "admin" "a1234")
  (let [company-id 
        (with-logged-user ["admin" "a1234"]
          (let [company-id (create-company! "D-Tel" "dtel")]
            (create-user-for-company! company-id {:email "a@dtel.pl" :first_name "Adam" :last_name "Kowalski"} "abcde")
            company-id))]
    

    (let [suppliers (with-logged-user ["a@dtel.pl" "abcde"]
                      [(create-supplier! company-id {:name "Druty Sp. z o.o."})
                       (create-supplier! company-id {:name "Kable Sp. z o.o."})])]
      (register-user-for-company-by-reg-code! (suppliers 0)
                                              {:email "adam@druty.pl"
                                               :password "abcde"
                                               :repeat-password "abcde"})
      (register-user-for-company-by-reg-code! (suppliers 0)
                                              {:email "adam@kable.pl"
                                               :password "abcde"
                                               :repeat-password "abcde"})
      )
    
    

  


  
  ))
  
  
  

    
