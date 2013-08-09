(ns finacentric.ctrl.company
  (:use [causeway.properties :only [defprop]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [causeway.validation]
        [causeway.l10n :only [loc]]
        [finacentric.model.companies]
        [finacentric.links])
  (:require [noir.session :as session]
            [noir.validation :as vali]))




(defvalidator register-company-validator
  (rule :regcode (get-company-with-regcode _) (loc "Niepoprawny klucz rejestracyjny"))
  (rule :name (<= 5 (count _) 40) (loc "Nazwa musi mieć między 5 a 40 znaków.")))

(defn register-company! [user-id {regcode :regcode company-name :name}]
  (if-let [company (get-company-with-regcode regcode)]
    (
    )
    (throw (Exception. "No company with given regcode"))))
