(ns finacentric.routes.admin
  (:use compojure.core)
  (:use finacentric.util
        finacentric.validation
        finacentric.forms)
  (:require [finacentric.views.layout :as layout]
            [finacentric.ajax :as ajax]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [noir.util.crypt :as crypt]
            [finacentric.models.db :as db]
            [hiccup.core :as hiccup]
            [korma.core :as korma]))




(defn layout [& content]
  (layout/render
   "admin/base.html" {:content (apply str (flatten content))}))

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
                                       (let [methods (if (fn? methods)
                                                       (methods o)
                                                       methods)]
                                             (for [m methods]
                                               (let [[k,v] (if (fn? m) (m o) m)]
                                                 (let [kname (name k)]
                                                   (if (-> kname last (= \!))
                                               [:a {:data-url (str (get o :id)"/"(subs kname 0 (dec (count kname))))
                                                    :class "ajaxify" :href "#"} v]
                                               [:a {:href (str (get o :id) "/" kname)} v])))
                                               )))]
                           ])]]))




(defn form-wrapper [content]
  (hiccup/html [:form {:method "post"}
                [:fieldset content]
                [:button {:type "submit" :class "btn"} "OK"]]))


(defmacro object-routes* [entity wrapper table-fields methods validator entity-form
                          {:keys [select select-one delete update insert]}]
  `(routes
     (GET "/list" []
       (with-pagination ~'page-no
         (with-page-size 30 ~'page-size
                         (layout
                          (~wrapper
                           (list (object-table ~table-fields ~methods (~select ~'page-no ~'page-size))
                                 ~@(when insert [`(hiccup/html [:a {:href "new"} "Nowy element"])]))
                           ~'page-no 10)
                                        ;page-no# (when id (db/select-one ~entity (korma/where {:id id}))))
                          ))))
    (id-context ~'id
      ~@(when delete
          [`(POST "/delete" []
              (~delete ~'id))])
      
      ~@(when update
          [`(GET "/edit" []
              (layout
               (~entity-form
                (~select-one ~'id) nil)))
           `(POST "/edit" {params# :params :as request#}
              (if-let [obj# (validates? ~validator params#)]
                (and (~update ~'id obj#)
                     (resp/redirect "../list"))
                (layout
                 (~entity-form params# (get-errors)))
                ))]))
    ~@(when insert
        [`(GET "/new" []
           (layout
            (~entity-form nil nil)))
         `(POST "/new" {params# :params :as request#}
           (if-let [obj# (validates? ~validator params#)]
             (and (~insert obj#)
                  (resp/redirect "list"))
             (layout
              (~entity-form params# (get-errors)))))])))

(defmacro object-routes [entity methods & args]
  `(object-routes* ~entity ~@args
                   ~(merge  {:select `#(korma/select ~entity (db/page % %2))
                             :select-one `#(db/select-one ~entity (korma/where {:id %}))
                             :delete `#(korma/delete ~entity (korma/where {:id %}))
                             :update `#(korma/update ~entity (korma/where {:id %1}) (korma/set-fields %2))
                             :insert `#(korma/insert ~entity (korma/values [%]))}
                            methods)))

(defmacro object-routes-read-only [entity methods & args]
  `(object-routes ~entity ~(merge {:delete nil :update nil :insert nil})  ~@args))

(defmacro object-routes-strict [entity methods & args]
  `(object-routes ~entity ~(merge {:select nil :select-one nil :delete nil :update nil :insert nil})  ~@args))

(defmacro object-routes-default [entity & args]
  `(object-routes ~entity {}  ~@args))


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
       (text-input :domain "Domena" 30)))))

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
       (text-input :email "Adres email" 50)))))

(def company-headers [[:id "#"] [:name "Nazwa"] [:domain "Domena"]])
(def user-headers [[:id "#"] [:first_name "Imię"] [:last_name "Nazwisko"] [:email "Email"] [:company_id "Firma"]])


(defn generic-wrapper [content page-no pages]
  (hiccup/html
   (list [:h1 "Lista"]
         content
         [:p "Strona " page-no " na " pages])))

(defn wrapper [header]
  (fn [content page-no pages]
     (hiccup/html
      (list [:h1 header]
            content
            [:p "Strona " page-no " na " pages]))))


(defroutes admin-routes
  (context "/admin" {:as request}
           (context "/companies" []
             (object-routes-default db/companies
                                    (wrapper "Lista firm")
                                    company-headers
                                    [[:delete! "Usuń"] [:edit "Edytuj"] ["users/list" "użytkownicy"]]
                                    valid-company company-form)
             (id-context company-id
               (context "/users" []
                 (context "/pin" [] (id-context user-id (POST "/company-pin" [] (db/users-pin-to-company user-id company-id)))
                   (object-routes-read-only db/users
                                  {:select #(korma/select db/users (korma/where
                                                                    (or (not= :company_id company-id)
                                                                        (= :company_id nil)
                                                                       )) (db/page % %2))
                                   :insert #(korma/insert db/users (korma/values [(assoc % :company_id company-id)]))}
                                   (wrapper "Wybierz użytkowników do przypięcia")
                                  user-headers
                                  [[:company-pin! "Przypnij"]]
                                  valid-user user-form))
                 
                 
                 (object-routes db/users 
                                 {:select #(korma/select db/users (korma/where {:company_id company-id}) (db/page % %2))
                                  :insert #(korma/insert db/users (korma/values [(assoc % :company_id company-id)])
                                                         )}
                                 (wrapper "Lista użytkowników firmy")
                                 user-headers
                                 [[:edit "Edytuj"] [:company-unpin! "Odepnij"]]
                                 valid-user user-form)
                 (id-context user-id
                   (POST "/company-unpin" []
                     (db/users-pin-to-company user-id nil))))))
           (context "/users" []
             (id-context user-id
               (POST "/admin/:val" [val]
                 (db/users-set-admin-state user-id (case val "set" true "unset" false)))
               (POST "/company-unpin" [] ;/admin/users/:user-id/company-unpin
                 (db/users-pin-to-company user-id nil)
                 
                 )
               (context "/companies" [] 
                 (id-context company-id (ajax/JSON "/company-pin" [] ;/admin/users/:user-id/companies/:company-id/company-pin
                                          (prn :!!!!)
                                          (db/users-pin-to-company user-id company-id)
                                          (ajax/redirect (str (current-url) "/../../../../list"))
                                          ))
                 (object-routes db/companies
                                {:update nil
                                 :insert #(db/create-company-for-user user-id %)}
                                (wrapper "Wybierz firmę do przypięcia")
                                company-headers
                                [[:company-pin! "Przypnij"]]
                                valid-user user-form))
               )

             
             (object-routes-default db/users
                                    (wrapper "Lista użytkowników")
                            user-headers
                            [[:delete! "Usuń"] [:edit "Edytuj"]
                             #(if (% :company_id)
                               ["company-unpin!" "Odepnij"]
                               ["companies/list" "Przypnij"])
                             #(if-not (% :admin)
                                ["admin/set!" "Uczyń adminem"]
                                ["admin/unset!" "Nie admin"])]
                            valid-user user-form))
           
           (context "/admins" []
             (object-routes-default db/admins
                                    (wrapper "Lista administratorów")
                                    user-headers
                                    [[:delete! "Usuń"] [:edit "Edytuj"]]
                                    valid-user user-form))))
