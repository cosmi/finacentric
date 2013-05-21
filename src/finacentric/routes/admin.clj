(ns finacentric.routes.admin
  (:use compojure.core)
  (:use finacentric.util
        finacentric.validation
        finacentric.forms)
  (:require [finacentric.views.layout :as layout]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [finacentric.models.db :as db]
            [hiccup.core :as hiccup]
            [korma.core :as korma]))

;; (defvalidator valid-user
;;    (rule :first_name (< 2 (count _) 5) "Za krutki penis, Kielce!")
;;    (when (not-empty (*input* :domain))
;;      (
;;    )


;; (defn valid-user [{:keys [first_name last_name email] :as params}]
;;   (vali/rule (<= 2 (count first_name) 30)
;;              [:first_name "Imię powinno mieć 2 do 30 znaków"])
;;   (vali/rule (<= 2 (count last_name) 40)
;;              [:last_name "Nazwisko powinno mieć 2 do 30 znaków"])
;;   (vali/rule (vali/is-email? email)
;;              [:email "Zły format adresu email"])
;;   (vali/rule (<= (count email) 50)
;;              [:last_name "Email nie powinien mieć więcej niż 50 znaków"])
;;   (when (not (vali/errors? :first_name :last_name :email))
;;     (select-keys params [:first_name :last_name :email])))


(defvalidator valid-company
  (rule :name (<= 5 (count _) 40) "Nazwa musi mieć między 5 a 40 znaków.")
  (when (not-empty (get-field :domain))
    (rule :domain (re-matches #"[a-z0-9]+" _) "Domena może się składać wyłącznie z małych liter oraz cyfr")
    (rule :domain (<= 4 (count _) 30) "Domena musi mieć między 4 a 30 znaków.")))



(defn companies [page-no & [params]]
  (layout/render
    "admin/companies.html" {:companies (korma/select db/companies (db/page page-no 50) (korma/order :id))
                            :e (get-errors)
                            :v params}))

;; (defn users [page-no & [{:as params}]]
;;   (layout/render
;;     "admin/users.html" {:users (korma/select db/companies (db/page page-no 50) (korma/order :id))
;;                         :errors {:domain  (vali/on-error :domain first)
;;                                  :name (vali/on-error :name first)}
;;                         :values params}))

(defn layout [content]
 (layout/render
  "admin/base.html" {:content content}))

(defn object-table [fields methods objects ]
  (hiccup/html [:table.table
                [:thead [:tr
                         (for [[k, v] fields] [:th v])
                         [:th "Metody"]
                         ]]
                [:tbody (for [o objects]
                          [:tr
                           (for [[k, v] fields]
                             [:td (get o k)])
                           [:td
                            (interpose " "
                                       (for [[k, v] methods]
                                         (if (-> k name last (= \!))
                                           [:a {:data-url (str (name k) "?id="(get o :id))
                                                :class "ajaxify" :href "#"} v]
                                           [:a {:href (str (name k) "?id="(get o :id))} v])
                                         ))]
                           ])]]))

(defn form-wrapper [content]
  (hiccup/html [:form {:method "post"}
                content
                [:button {:type "submit" :class "btn"} "OK"]]))
;  <fieldset>
 ;   <legend>Dodawanie domeny </legend>

(defn company-form [input]
  (form-wrapper 
   (with-input input
     (text-input :name "Nazwa" 40)
     (text-input :domain "Domena" 30)
     )))



(defmacro object-routes [entity table-fields methods]
  (let [table-fields (eval table-fields)
        methods (eval methods)]
    `(routes
       (GET "/list" []
         (with-pagination page-no#
           (layout
            (object-table ~table-fields ~methods
                          (korma/select ~entity (db/page page-no# 50)))
                                        ;page-no# (when id (db/select-one ~entity (korma/where {:id id}))))
            )))
       ~@(when (get methods :delete!)
          [`(POST "/delete" []
             (with-integer ~'id
               (korma/delete ~entity (korma/where {:id ~'id}))))])

       ~@(when (get methods :edit)
          [`(GET "/edit" []
             (with-integer ~'id
               (layout
                (company-form
                 (db/select-one ~entity (korma/where {:id ~'id}))))))])
       )))



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
    (context "/cps" []
      (object-routes db/companies
                     (array-map :id "#" :name "Nazwa" :domain "Domena")
                     (array-map :delete! "Usuń" :edit "Edytuj")))

    (POST "/companies" {params :params}
      (with-pagination page-no
        (with-integer id
          (if-let [company (validates? valid-company params)]
            (try
              (do
                (if id
                  (db/update-company id company)
                  (db/create-company company))
                (resp/redirect (request :uri)))
              (catch Exception ex
                (set-error! :name (.getMessage ex))
                (companies page-no params)))
            (companies page-no params)
            ))))))
  
