(ns finacentric.ctrl.company
  (:use [causeway.properties :only [defprop]]
        [causeway.bootconfig :only [devmode? bootconfig]]
        [causeway.validation]
        [causeway.l10n :only [loc]]
        [finacentric.model.users]
        [finacentric.links])
  (:require [noir.session :as session]
            [noir.validation :as vali]))


(defn get-company-by-regcode [regcode]

  )


(defvalidator register-company-validator
  (rule :regcode (get-company-by-regcode _) "Niepoprawny klucz rejestracyjny")
  (rule :name (<= 5 (count _) 40) "Nazwa musi mieć między 5 a 40 znaków.")
  )

(defn register-company! [{regcode :regcode company-name :name}]
  (let [company (get-company-by-regcode regcode)]
    
    ))
