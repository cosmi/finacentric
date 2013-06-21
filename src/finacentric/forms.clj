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

(defn text-input [field label max-len]
  (hiccup/html
   (list
    [:label label]
    [:input {:type "text" :name (get-field-name field) :value (get-value field) :maxlength max-len}]
    (when-let [error (get-error field)]
      [:div.error error]
      ))))


(defn hidden-input [field]
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
  `(binding [*context* (conj *context* ~context)]
     ~@body))

