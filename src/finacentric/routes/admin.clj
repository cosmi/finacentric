(ns finacentric.routes.admin
  (:use compojure.core)
  (:use finacentric.util)
  (:require [finacentric.views.layout :as layout]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [finacentric.models.db :as db]
            [korma.core :as korma]))





(defn valid-company [{:keys [name domain]}]
  (vali/rule (vali/min-length? name 5)
             [:name "Nazwa musi mieć conajmniej pięć literek"])
  (vali/rule (vali/max-length? name 40)
             [:name "Nazwa może mieć maksymalnie 40 literek"])
  (when (not-empty domain)
    (vali/rule (vali/min-length? domain 4)
               [:domain "Domena musi mieć 4 literki"])
    (vali/rule (vali/max-length? domain 30)
               [:domain "Domena musi mieć co najwyżej 30 literek"])
    (vali/rule (re-matches #"[a-z0-9]+" domain)
               [:domain "Domena może się składać wyłącznie z małych liter oraz cyfr"]))
  (when (not (vali/errors? :name :domain))
    {:name name :domain (not-empty domain)}))



(defn companies [page-no & [{:keys [name domain] :as params}]]
  (layout/render
    "admin/companies.html" {:companies (korma/select db/companies (db/page page-no 50) (korma/order :id))
                            :errors {:domain  (vali/on-error :domain first)
                                     :name (vali/on-error :name first)}
                            :values params}))

(defroutes admin-routes
  (context "/admin" {:as request}
    (GET "/companies" []
      (with-pagination page-no
        (with-integer id
          (companies page-no (when id (db/select-one db/companies (korma/where {:id id}))))
        )))

    (POST "/companies/delete" {params :params}
      (with-integer id
        (korma/delete db/companies (korma/where {:id id}))
        ))

    (POST "/companies" {params :params}
      (with-pagination page-no
        (with-integer id
          (let [params (select-keys params [:name :domain])]
            (if-let [company (valid-company params)]
              (try
                (do
                  (if id
                    (db/update-company id company)
                    (db/create-company company))
                  (resp/redirect (request :uri)))
                (catch Exception ex
                  (vali/rule false [:name (.getMessage ex)])
                  (companies page-no params)))
              (companies page-no params)
              )))))))
  
