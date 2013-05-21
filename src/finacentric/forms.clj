(ns finacentric.forms
  (:use compojure.core)
  (:use finacentric.util)
  (:require [noir.validation :as vali]
            [hiccup.core :as hiccup]))

(def ^{:dynamic true :private true} *input*)
(def ^{:dynamic true :private true} *context* [])

(defn get-value [field]
  (-> (get-in *input* *context*) (get field)))

(defn name-field [field]
  (if (empty? *context*)
    (name field)
    (apply str
           (name (first *context*))
           (map #(str "[" (name %) "]") (rest (conj *context* field))))))

(defn text-input [field label max-len]
  (fn [value error]
    (hiccup/html
     (list
      [:label label]
      [:input {:type "text" :name (name-field field) :value (get-value field) :maxlength max-len}]))))

(defmacro with-input [input & body]
  `(binding [*input* ~input]
     (str
      ~@body)))

(defmacro in-context [context & body]
  `(binding [*context* (conj *context* ~context)]
     ~@body))