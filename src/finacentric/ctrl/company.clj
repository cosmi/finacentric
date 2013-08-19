(ns finacentric.ctrl.company
  (:use [causeway.properties :only [defprop]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [causeway.validation]
        [causeway.l10n :only [loc]]
        [finacentric.model companies users regcodes]
        [finacentric.ctrl auth]
        [finacentric.links]
        [korma.db :only [transaction rollback]])
  (:require [noir.session :as session]
            [noir.validation :as vali]
            [causeway.status :as status]))




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
  (optional
   (rule :name (<= 5 (count _) 80) (loc "Nazwa powinna mieć między 1 a 80 znaków.")))
  (rule :email (vali/is-email? _) (loc "Niepoprawny format adresu email"))
  (rule :email (<= (count _) 50) (loc "Adres email nie powinien mieć więcej niż 50 znaków"))
  )

(defn create-invite! [{:keys [name email]}]
  (create-regcode! (get-current-company-id) name email))




(defn get-current-suppliers-list [page order dir]
  (get-suppliers  (get-current-company-id) page 30 order dir))
                                                                         
(defn get-current-regcodes-list [page sort-order dir]
  (get-regcodes  (get-current-company-id) page 30 sort-order dir))



(defn delete-invite! [regcode-id]
  (transaction
   (if (check-regcode-auth regcode-id (get-current-company-id) :delete)
     (delete-regcode! regcode-id)
     (throw status/forbidden))))



