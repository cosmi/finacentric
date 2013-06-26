(ns finacentric.forms
  (:use compojure.core)
  (:use finacentric.util)
  (:require [noir.validation :as vali]
            [hiccup.core :as hiccup]))

(def ^{:dynamic true :private true} *input*)
(def ^{:dynamic true :private true} *errors*)
(def ^{:dynamic true :private true} *context* [])

(defn get-value [field]
  (-> (get-in *input* *context*) (get field)))
(defn get-error [field]
  (-> (get-in *errors* *context*) (get field)))

(defn get-field-name [field]
  (if (empty? *context*)
    (name field)
    (apply str
           (name (first *context*))
           (map #(str "[" (name %) "]") (rest (conj *context* field))))))

(defn file-input [field label]
  (hiccup/html
   (list
    [:label label]
    [:input {:type "file" :name (get-field-name field)}]
    (when-let [error (get-error field)]
      [:div.error error]))))

(defn text-input
  ([field label max-len disabled?]
     (hiccup/html
      (list
       [:label label]
       [:input (cond->
                {:type "text" :name (get-field-name field) :value (get-value field) :maxlength max-len}
                disabled?
                (assoc :disabled true))]
       (when-let [error (get-error field)]
         [:div.error error]))))
  ([field label max-len]
     (text-input field label max-len false)))


(defn hidden-input [field]
  (when-let [error (get-error field)]
    (throw (ex-info "Hidden field error" {:error error :field field})))
  (hiccup/html
   [:input {:type "hidden" :name (get-field-name field) :value (get-value field)}]))


(defn pass-input [field label max-len]
  (hiccup/html
   (list
    [:label label]
    [:input {:type "password" :name (get-field-name field) :value (get-value field) :maxlength max-len}]
    (when-let [error (get-error field)]
      [:div.error error]
      ))))



(defn pass-input*
  "Without input"
  [field label max-len]
  (hiccup/html
   (list
    [:label label]
    [:input {:type "password" :name (get-field-name field) :maxlength max-len}]
    (when-let [error (get-error field)]
      [:div.error error]
      ))))


(defn compare-vals [a b]
  (or (and (nil? a) (nil? b))
      (when (and a b)
        (or (= a b)
            (= (name a) (name b))))))
(defn select-input
  ([field label values disabled?]
     (hiccup/html
      (list
       [:label label]
       [:select (cond->
                {:name (get-field-name field)}
                disabled?
                (assoc :disabled true))
        (for [[k, lab] values]
          [:option (cond-> {:value (name k)}
                           (compare-vals k (get-value field))
                           (assoc :selected true)) lab])]
       (when-let [error (get-error field)]
         [:div.error error]))))
  ([field label values]
     (select-input field label values false)))

(defn radio-input
  ([field label values disabled?]
     (hiccup/html
      (list
       [:label label]
       (for [[k, lab] values]
         [:label
          [:input (cond->
                   {:type "radio"
                    :name (get-field-name field)
                    :value (name k)}
                   disabled?
                   (assoc :disabled true)
                   (compare-vals k (get-value field))
                   (assoc :checked true)
                   )]
          lab])
       (when-let [error (get-error field)]
         [:div.error error]))))
  ([field label values]
     (radio-input field label values false)))



(def date-input text-input) ;TODO
(def decimal-input text-input) ;TODO

(defmacro with-input [input & body]
  `(binding [*input* ~input]
     (list
      ~@body)))

(defmacro with-errors [errors & body]
  `(binding [*errors* ~errors]
     (list
      ~@body)))

(defmacro in-context [context & body]
  `(binding [*context* (conj @#'*context* ~context)]
     (list
      ~@body)))

