(ns finacentric.testing.fixtures
  (:use [lobos.core :only (defcommand migrate)])
  (:require [clojure.java.jdbc :as sql]
            [lobos.migration :as lm]
            [clj-http.client :as http]))


(defn form-post
  [url args]
  (http/post (str "http://localhost:3000" url)  {:form-params args}))

(defn admin-post
  [url args]
  (form-post (str "/admin" url) args))


(defn create-company [name & [domain]]
  (admin-post "/companies/new" {:name name :domain domain}))





(def companies
  [["D-Tel" "dtel"]
   ["B-Tel" "btel"]
   ["Sznurki"]
   ["Kable"]])
  



(defn init-db []
  (lobos.core/reset)
  (doseq [a companies] (apply create-company a)))
  
  
  

    
