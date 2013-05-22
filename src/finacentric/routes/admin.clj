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



;; (defn companies [page-no & [params]]
;;   (layout/render
;;     "admin/companies.html" {:companies (korma/select db/companies (db/page page-no 50) (korma/order :id))
;;                             :e (get-errors)
;;                             :v params}))

(defn layout [& content]
  (layout/render
   "admin/base.html" {:content (apply str content)}))

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
                                       (for [m methods]
                                         (let [[k,v] (if (fn? m) (m o) m)]
                                           (let [kname (name k)]
                                             (if (-> kname last (= \!))
                                               [:a {:data-url (str (get o :id)"/"(subs kname 0 (dec (count kname))))
                                                    :class "ajaxify" :href "#"} v]
                                               [:a {:href (str (get o :id) "/" kname)} v])))
                                         ))]
                           ])]]))




(defn form-wrapper [content]
  (hiccup/html [:form {:method "post"}
                [:fieldset content]
                [:button {:type "submit" :class "btn"} "OK"]]))


(defmacro object-routes [entity table-fields methods validator entity-form]
  `(routes
     (GET "/list" []
       (with-pagination page-no#
         (layout
          (object-table ~table-fields ~methods
                        (korma/select ~entity (db/page page-no# 50)))
          (hiccup/html [:a {:href "new"} "Nowy element"])
                                        ;page-no# (when id (db/select-one ~entity (korma/where {:id id}))))
          )))
     (context ["/:id", :id #"[0-9]+"]  {{~'id :id} :params :as request#}
       (with-integer ~'id
         (prn :!!id ~'id (-> noir.request/*request* ))
         (routes
           ~@(when (not-empty (filter #(when (seq? %) (-> % first (= :delete!) )) methods))
               [`(POST "/delete" []
                   (korma/delete ~entity (korma/where {:id ~'id})))])

           ~@(when (not-empty (filter #(when (seq? %) (-> % first (= :edit) )) methods))
               [`(GET "/edit" []
                   (prn :IDD ~'id)
                   (layout
                    (~entity-form
                     (db/select-one ~entity (korma/where {:id ~'id})) nil)))
                `(POST "/edit" {params# :params :as request#}
                   (if-let [obj# (validates? ~validator params#)]
                     (and (korma/update ~entity (korma/where {:id ~'id}) (korma/set-fields obj#))
                          (resp/redirect "list"))
                     (layout
                      (~entity-form params# (get-errors)))
                     ))]))))

     (GET "/new" []
       (layout
        (~entity-form nil nil)))
     (POST "/new" {params# :params :as request#}
       (if-let [obj# (validates? ~validator params#)]
         (and (korma/insert ~entity (korma/values [obj#]))
              (resp/redirect "list"))
         (layout
          (~entity-form params# (get-errors)))
         ))))





(defvalidator valid-company
  (rule :name (<= 5 (count _) 40) "Nazwa musi mieć między 5 a 40 znaków.")
  (when (not-empty (get-field :domain))
    (rule :domain (re-matches #"[a-z0-9]+" _) "Domena może się składać wyłącznie z małych liter oraz cyfr")
    (rule :domain (<= 4 (count _) 30) "Domena musi mieć między 4 a 30 znaków.")))

(defn company-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (text-input :name "Nazwa" 40)
       (text-input :domain "Domena" 30)
       ))))


(defvalidator valid-user
  (rule :first_name (<= 2 (count _) 30) "Imię powinno mieć 2 do 30 znaków")
  (rule :last_name (<= 2 (count _) 40) "Nazwisko powinno mieć 2 do 40 znaków")
  (rule :email (vali/is-email? _) "Niepoprawny format adresu email")
  (rule :email (<= (count _) 50) "Email nie powinien mieć więcej niż 50 znaków"))

(defn user-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (text-input :first_name "Imię" 30)
       (text-input :last_name "Nazwisko" 40)
       (text-input :email "Adres email" 50)
       ))))





(defroutes admin-routes
  (context "/admin" {:as request}
    ;; (GET "/companies" []
    ;;   (with-pagination page-no
    ;;     (with-integer id
    ;;       (companies page-no (when id (db/select-one db/companies (korma/where {:id id}))))
    ;;     )))

    ;; (POST "/companies/delete" {params :params}
    ;;   (with-integer id
    ;;     (korma/delete db/companies (korma/where {:id id}))
    ;;     ))
    (context "/companies" []
      (object-routes db/companies
                     (array-map :id "#" :name "Nazwa" :domain "Domena")
                     (array-map :delete! "Usuń" :edit "Edytuj")
                     valid-company company-form))
    (context "/users" []
      (object-routes db/users
                     [[:id "#"] [:first_name "Imię"] [:last_name "Nazwisko"] [:email "Email"]]
                     [[:delete! "Usuń"] [:edit "Edytuj"] #(if-not (% :admin)
                                                            [:admin-set! "Uczyń adminem"]
                                                            [:admin-unset! "Nie admin"])]
                     valid-user user-form)

     (context ["/:id", :id #"[0-9]+"] [id]
       (with-integer id
         (routes
           (POST "/admin-set" []
             (korma/update db/users (korma/where {:id id})
                           (korma/set-fields {:admin true}))
             )
           (POST "/admin-unset" []
             (korma/update db/users (korma/where {:id id})
                           (korma/set-fields {:admin false}))
             )
           ))))))

    ;; (POST "/companies" {params :params}
    ;;   (with-pagination page-no
    ;;     (with-integer id
    ;;       (if-let [company (validates? valid-company params)]
    ;;         (try
    ;;           (do
    ;;             (if id
    ;;               (db/update-company id company)
    ;;               (db/create-company company))
    ;;             (resp/redirect (request :uri)))
    ;;           (catch Exception ex
    ;;             (set-error! :name (.getMessage ex))
    ;;             (companies page-no params)))
    ;;         (companies page-no params)
    ;;         ))))))
  
