(ns finacentric.ctrl.company
  (:use [causeway.properties :only [defprop]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [causeway.validation]
        [causeway.l10n :only [loc]]
        [finacentric.model companies users]
        
        [finacentric.links]
        [korma.db :only [transaction rollback]])
  (:require [noir.session :as session]
            [noir.validation :as vali]))




(defvalidator register-supplier-validator
  (rule :regcode (get-company-regcode _) (loc "Niepoprawny klucz rejestracyjny"))
  (rule :name (<= 5 (count _) 80) (loc "Nazwa musi mieć między 5 a 80 znaków.")))

(defn register-supplier! [user-id {regcode :regcode company-name :name}]
  (transaction
   (if-let [regcode (get-company-regcode regcode)]
     (let [supplier (create-supplier! (regcode :owner_company_id) {:name company-name})]
       (use-up-regcode! (regcode :id) (supplier :id))
       (set-users-company-id! user-id (supplier :id)))
     (throw (Exception. "Invalid regcode")))))





(defvalidator register-buyer-validator
  (rule :name (<= 5 (count _) 80) (loc "Nazwa musi mieć między 5 a 80 znaków.")))


(defn register-buyer! [user-id {company-name :name}]
  (transaction
   (let [company (create-buyer-company! company-name)]
     (set-users-company-id! user-id (company :id)))))



(defvalidator invite-supplier-validator
  (rule :email (<= 5 (count _) 80) (loc "Nazwa musi mieć między 5 a 80 znaków."))
  )
