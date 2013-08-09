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




(defvalidator register-company-validator
  (rule :regcode (get-company-with-regcode _) (loc "Niepoprawny klucz rejestracyjny"))
  (rule :name (<= 5 (count _) 80) (loc "Nazwa musi mieć między 5 a 80 znaków.")))

(defn register-company! [user-id {regcode :regcode company-name :name}]
  (if-let [company (get-company-with-regcode regcode)]
    (transaction
     (when-not 
         (and (set-users-company-id! user-id (company :id))
              (finalize-company! (company :id) company-name))
       (do (rollback)
           (throw (Exception. "Cannot finalize company"))
           ))
    (throw (Exception. "No company with given regcode")))))





(defvalidator register-buyer-validator
  (rule :name (<= 5 (count _) 80) (loc "Nazwa musi mieć między 5 a 80 znaków.")))


(defn register-buyer! [user-id {company-name :name}]
  (transaction
     (when-not 
         (when-let [company (create-buyer-company! company-name)]
           (set-users-company-id! user-id (company :id)))
       (rollback))))
