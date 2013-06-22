(ns finacentric.routes.supplier
  (:use compojure.core)
  (:use clojure.pprint)
  (:use finacentric.util
        finacentric.validation
        finacentric.forms)
  (:require [finacentric.views.layout :as layout]
            [finacentric.models.invoices :as invoices]
            [finacentric.ajax :as ajax]
            [finacentric.routes.auth :as auth]
            [finacentric.files :as files]
            [noir.session :as session]
            [noir.response :as resp]
            [noir.validation :as vali]
            [finacentric.validation-utils :as vali-util]
            [noir.util.crypt :as crypt]
            [finacentric.models.db :as db]
            [hiccup.core :as hiccup]
            [korma.core :as korma])
  
  (:require [clj-time.core :as time]
            [clj-time.coerce :as coerce]))

(def INVOICE-FILE-SIZE-LIMIT (* 150 1024))

(def ^:dynamic *context* nil)

(defn layout [& content]
  (layout/render
   "app/sup_base.html" {:content (apply str (flatten content))}))

(defn prepare-invoices [from to page per-page]
  (db/get-invoices from to (db/page-filter page per-page)))


(defn dashboard [supplier-id buyer-id page per-page]
  ;(binding [*context* (str "/supplier/" supplier-id "/" buyer-id)]
  (layout/render
   "app/sup_dashboard.html" {:invoices
                             (->>
                              (db/get-invoices
                               supplier-id buyer-id
                               (db/page-filter page per-page))
                              (map invoices/append-state)
                              )}))


(defn invoice-view [invoice-id supplier-id buyer-id invoice]
  (layout/render
   "app/sup_invoice.html" {:i (assoc invoice :id invoice-id)}))


(defn form-wrapper ([attrs content]
  (hiccup/html [:form (merge {:method "post" :enctype "multipart/form-data"}
                             attrs)
                [:fieldset content]
                [:button {:type "submit" :class "btn"} "OK"]]))
  ([content]
     (form-wrapper {} content)))

;; Invoice Form

(defvalidator valid-simple-invoice
  (rule :number (<= 2 (count _) 40) "Numer powinien mieć 2 do 40 znaków")
  (date-field :issue_date "Błędny format daty")
  (date-field :sell_date "Błędny format daty")
  (date-field :payment_date "Błędny format daty")
  (rule :net_total (<= (count _) 50) "Zbyt długi ciąg")
  (rule :gross_total (<= (count _) 50) "Zbyt długi ciąg")
  (decimal-field :net_total 2 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")
  (decimal-field :gross_total 2 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")
  (convert :invoice (if (vali/valid-file? _) _ nil))
  (optional
    (with-field :invoice
      (rule (= "application/pdf" (:content-type _)) "Niepoprwany typ dokumentu")
      (rule (<= (:size _) INVOICE-FILE-SIZE-LIMIT) "Plik z fakturą jest zbyt duży"))))

(defn simple-invoice-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       ;; opcjonalne pola mogą zepsuć edycję chyba (?)
       (text-input :number "Numer" 40)
       (date-input :issue_date "Data wystawienia" 30)
       (date-input :sell_date "Data sprzedaży" 10)
       (date-input :payment_date "Termin płatności" 30)
       (decimal-input :net_total "Wartość netto" 30)
       (decimal-input :gross_total "Wartość brutto" 30)
       (file-input :invoice "Elektroniczna faktura (PDF)")))))

(defn- handle-simple-invoice-form [input supplier-id buyer-id]
  ;; file upload?
  (let [upload (when (input :invoice)
                 (try
                   (files/store-file! (-> input :invoice :tempfile)
                                      (-> input :invoice :content-type) {})
                   (catch Exception e (.printStackTrace e) nil)))
        input (dissoc (if upload (assoc input :file_id upload) input) :invoice)]
    (and (input :invoice) (not upload)
         (throw-validation-error :invoice "Błąd podczas zapisywania pliku"))
    (db/simple-create-invoice input supplier-id buyer-id)))

(defn- handle-simple-invoice-form-edit [input supplier-id invoice-id ]
  ;; file upload? TODO: wyciagnac upload do osobnego pliki (może nawet to zrobić jako walidator cośtam input).
  (let [upload (when (input :invoice)
                 (try
                   (files/store-file! (-> input :invoice :tempfile)
                                      (-> input :invoice :content-type) {})
                   (catch Exception e (.printStackTrace e) nil)))
        input (dissoc (if upload (assoc input :file_id upload) input) :invoice)]
    (and (input :invoice) (not upload)
         (throw-validation-error :invoice "Błąd podczas zapisywania pliku"))
    (invoices/invoice-simple-edit! supplier-id invoice-id input)))


;; Invoice Correction Form

(defvalidator valid-correction-invoice
  (date-field :payment_date "Błędny format daty")
  (rule :net_total (<= (count _) 50) "Zbyt długi ciąg")
  (rule :gross_total (<= (count _) 50) "Zbyt długi ciąg")
  (decimal-field :net_total 2 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")
  (decimal-field :gross_total 2 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")

  (rule :gross_total (< (get-field :net_total) _) "Wartość brutto jest mniejsza niż netto")
  (convert :invoice (if (vali/valid-file? _) _ nil))
  (optional
    (with-field :invoice
      (rule (= "application/pdf" (:content-type _)) "Niepoprwany typ dokumentu")
      (rule (<= (:size _) INVOICE-FILE-SIZE-LIMIT) "Plik z fakturą jest zbyt duży"))))

(defn correction-invoice-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (text-input :number "Numer" 40 true)
       (date-input :issue_date "Data wystawienia" 30 true)
       (date-input :sell_date "Data sprzedaży" 10 true)
       (date-input :payment_date "Termin płatności" 30 true)
       (decimal-input :net_total "Wartość netto" 30)
       (decimal-input :gross_total "Wartość brutto" 30)
       (file-input :invoice "Korekta faktury (PDF)")))))

(defn- handle-correction-invoice-form [input supplier-id buyer-id]
  ;; file upload?
  ;; TODO na razie to ma w kontent skopiowany z tworzenia inwojsa
  (let [upload (when (input :invoice)
                 (try
                   (files/store-file! (-> input :invoice :tempfile)
                                      (-> input :invoice :content-type) {})
                   (catch Exception e (.printStackTrace e) nil)))
        input (dissoc (if upload (assoc input :file_id upload) input) :invoice)]
    (and (input :invoice) (not upload)
         (throw-validation-error :invoice "Błąd podczas zapisywania pliku"))
    (db/simple-create-invoice input supplier-id buyer-id)))



;;; REGISTER FROM REG-CODE
(defvalidator register-from-reg-code-validator
  (rule :email (vali/is-email? _) "Niepoprawny format adresu email")
  (rule :email (<= (count _) 50) "Email nie powinien mieć więcej niż 50 znaków")
  (optional
    (rule :first_name (<= 2 (count _) 30) "Imię powinno mieć 2 do 30 znaków")
    (rule :last_name (<= 2 (count _) 40) "Nazwisko powinno mieć 2 do 40 znaków"))
  (rule :password (<= 5 (count _) 50) "Hasło powinno mieć 5 do 50 znaków")
  (rule :password (re-matches #"[a-zA-Z0-9-_!@#$%^&*]*" _) "Hasło zawiera niedozwolone znaki")
  (rule :repeat-password (= (get-field :password) _) "Hasła się nie zgadzają")
  (rule :reg-code (< 0 (count _)) "Pole obowiązkowe"))

(defn register-from-reg-code-form [input errors]
  (form-wrapper
   (with-input input
     (with-errors errors
       (text-input :reg-code "Kod rejestracyjny *" 50)
       (text-input :email "Adres email *" 50)
       (pass-input* :password "Hasło *" 50)
       (pass-input* :repeat-password "Powtórz hasło *" 50)
       (text-input :first_name "Imię" 30)
       (text-input :last_name "Nazwisko" 40)))))

;; Routes

(defn FORM-register-to-company []
  (FORM "/register"
        (fn [input errors] (layout (register-from-reg-code-form input errors)))
        register-from-reg-code-validator
        #(let [id (try
                    (db/create-user-from-reg-code!
                     (% :reg-code)
                     (dissoc % :reg-code :repeat-password :password)
                     (% :password))
                    (catch clojure.lang.ExceptionInfo e#
                      (throw-validation-error :reg-code
                                              "Niepoprawny kod rejestracyjny")))]
           (resp/redirect (str "supplier")))
        "."
        ))

(defn FORM-simple-invoice [supplier-id buyer-id]
  (FORM "/simple-invoice-form"
        #(layout (simple-invoice-form %1 %2))
        valid-simple-invoice
        #(handle-simple-invoice-form % supplier-id buyer-id)
        "hello"))

(defn FORM-simple-invoice-edit [supplier-id invoice-id]
  (routes-when (invoices/check-invoice
                invoice-id
                (invoices/has-state? :input :rejected))
  (FORM "/edit"
        (fn [inp errors]
          (let [inp (if (nil? inp)
                      (db/get-invoice-unchecked invoice-id)
                      inp)]
            (layout (simple-invoice-form inp errors))))
        valid-simple-invoice
        #(handle-simple-invoice-form-edit % supplier-id invoice-id)
        ".")))

;; DISCOUNT ACCEPT FORM
(defvalidator valid-discount-accept-form
  (date-field :discounted_payment_date "Błędny format daty")
  (date-field :earliest_discount_date "Błędny format daty")
  
  (date-field :payment_date "Błędny format daty")
  (rule :discounted_payment_date
        (-> (coerce/from-sql-date _)
            (.compareTo (coerce/from-sql-date (get-field :earliest_discount_date)))
            (>= 0)) "Kontrahent nie godzi się na tak wczesną datę płatności.")
  (rule :discounted_payment_date
        (-> (coerce/from-sql-date _)
            (.compareTo (coerce/from-sql-date (get-field :payment_date)))
            (< 0)) "Nowa data musi być wcześniejsza, niż oryginalna data płatności.")
  (decimal-field :annual_discount_rate 4 "Błędny format danych"
                 "Wartość nie powinna mieć więcej niż 2 miejsca po przecinku")
  )

(defn discount-accept-form [invoice-id input errors]
  (form-wrapper
   {:class "on-change-ajaxify" :data-url "calculate-discount"}
   (with-input input
     (with-errors errors
       (hiccup/html (list
                     [:p "Najwcześniejsza możliwa data płatności: "
                        (input :earliest_discount_date)]
                     [:p "Data płatności na fakturze: "
                      (input :payment_date)]
                     [:p "Koszt przyspieszonej płatności: "
                      (input :annual_discount_rate)
                      "% (w skali roku)"]

                     [:p "Wartość netto po obniżce: "
                      [:span {:class "target discounted_net_total"} "[Wpisz datę poniżej]"]]
                     [:p "Wartość brutto po obniżce: "
                      [:span {:class "target discounted_gross_total"} "[Wpisz datę poniżej]"]]
                     
                     ))
       (date-input :discounted_payment_date "Data wystawienia" 30)
       (hidden-input :annual_discount_rate)
       (hidden-input :earliest_discount_date)
       (hidden-input :payment_date)
       ))))


(defn FORM-discount-accept [invoice-id supplier-id]
  (routes-when (invoices/check-invoice
                invoice-id
                (invoices/has-state? :discount_offered :discount_accepted))
    (ajax/JSON "/calculate-discount" {params :params}
          (if-let [params (validates? discount-accept-form params)]
            (ajax/write-vals (invoices/get-discount-values invoice-id (params :discounted_payment_date)))
            []))
    (FORM "/discount-accept"
          (fn [input errors]
            (let [input (if (nil? input)
                          (db/get-invoice-unchecked invoice-id)
                          input
                          )]
              (layout (discount-accept-form invoice-id input errors))))
          valid-discount-accept-form
          #(try
             (invoices/invoice-accept-discount!
              supplier-id invoice-id
              (% :annual_discount_rate)
              (% :discounted_payment_date)
              (% :earliest_discount_date))
             (resp/redirect ".")
             (catch Exception e
               (.printStackTrace e)
               (throw-validation-error :discounted_payment_date
                                       "Nastąpił błąd, spróbuj ponownie."))))))




(defn FORM-correction-invoice [supplier-id buyer-id]
  (FORM "/correction"
        #(layout (correction-invoice-form %1 %2))
        valid-correction-invoice
        #(handle-correction-invoice-form % supplier-id buyer-id)))

(defn invoice-details [supplier-id buyer-id invoice-id]
  (when-let [invoice (db/get-invoice invoice-id supplier-id buyer-id)]
    (invoice-view invoice-id supplier-id buyer-id invoice)))

(defn invoice-file [supplier-id buyer-id invoice-id]
  (when-let [invoice (db/get-invoice invoice-id supplier-id buyer-id)]
    (when (invoice :file_id)
      (when-let [file (files/get-file (invoice :file_id))]
        (files/response file (str (invoice :number) ".pdf"))))))






(defroutes supplier-routes
  (context "/supplier" {:as request}
    (GET "/" []
      (resp/redirect (str "/supplier/" (or (auth/get-current-users-company-id) 0)))) 
    (FORM-register-to-company)
    (with-int-param [supplier-id (auth/get-current-users-company-id)]
      (routes-when (auth/logged-to-company? supplier-id)
        (with-int-param [buyer-id (db/get-suppliers-first-buyer-id supplier-id)]
          (routes-when buyer-id
            (GET "/hello" []
              (with-pagination page-no
                (with-page-size 30 page-size
                  (dashboard supplier-id buyer-id page-no page-size))))
            (FORM-simple-invoice supplier-id buyer-id)

            (context "/invoice" []
              (id-context invoice-id
                (routes-when (invoices/check-invoice
                              invoice-id
                              (invoices/is-seller? supplier-id))
                  
                  (GET "/" []
                    (invoice-details supplier-id buyer-id invoice-id))

                  (routes-when (invoices/check-invoice
                                invoice-id
                                (invoices/has-state? :discount_confirmed :correction_done :correction_received))
                    (FORM-correction-invoice supplier-id buyer-id))

                  (FORM-simple-invoice-edit invoice-id supplier-id)
                  (FORM-discount-accept invoice-id supplier-id)
                  
                
                  (GET "/file" []
                    (invoice-file supplier-id buyer-id invoice-id)))))))))))


