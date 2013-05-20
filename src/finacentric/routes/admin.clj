(ns finacentric.routes.admin
  (:use compojure.core)
  (:require [finacentric.views.layout :as layout]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [finacentric.models.db :as db]
            [korma.core :as korma]))



(defn valid-domain? [name domain]
  (vali/rule (vali/min-length? name 5)
             [:name "Nazwa musi mieć conajmniej pięć literek"])
  (vali/rule (vali/max-length? name 40)
             [:name "Nazwa może mieć maksymalnie 40 literek"])
  (vali/rule (vali/min-length? domain 4)
             [:domain "Domena musi mieć 4 literki"])
  (vali/rule (vali/max-length? domain 30)
             [:domain "Domena musi mieć co najwyżej 30 literek"])
  (not (vali/errors? :name :domain)))



(defn domains [& [name domain]]
  (layout/render
    "admin/domains.html" {:domains (korma/select db/domains)
                          :domain-error (vali/on-error :domain first)
                          :name-error (vali/on-error :name first)
                          :name name
                          :domain domain}))

(defroutes admin-routes
  (context "/admin" {:as request}
    (GET "/domains" []
      (domains)
      )

    (POST "/domains" [name domain]
       (if (valid-domain? name domain)
         (try
           (do
             (db/create-domain {:name name :domain domain})
             (resp/redirect (request :uri)))
           (catch Exception ex
             (vali/rule false [:name (.getMessage ex)])
             (domains)))
         (domains name domain)
         ))

    )
  )
