(ns finacentric.validation
  (:use compojure.core)
  (:use finacentric.util)
  (:require [noir.validation :as vali]))




(def ^:dynamic *errors*)
(def ^:dynamic *input*)
(def ^:dynamic *output*)
(def ^:dynamic *context* [])


(defn set-error! [field text]
  (swap! *errors* assoc-in (conj *context* field) text))

(defn set-value! [field value]
  (swap! *input* assoc-in (conj *context* field) value)
  (swap! *output* assoc-in (conj *context* field) value))

(defmacro rule [field test error-msg]
  `(let [~'_ (get @*input* ~field)]
     (if (binding [*context* (conj *context* ~field)]
           ~test)
       (set-value! ~field ~'_)
       (set-error! ~field ~error-msg))))


(defmacro convert [field test]
  `(let [~'_ (get @*input* ~field)]
     (binding [*context* (conj *context* ~field)]
       (let [res# ~test]
         (set-value! ~field res#)))))



(defmacro validator [& rules]
  `(fn [input#]
     (binding [*input* (atom input#)
               *output* (atom {})]
       ~@rules
       @*output*
       )))


(defmacro defvalidator [name & rules]
  `(def ~name (validator ~@rules)))


(defn validates? [validator input]
  (let [res (validator input)]
    (when (empty? @*errors*)
      res)))

(defn get-errors []
  (-> *errors* deref (get-in *context*)))

(defn get-field [field]
  (-> *input* deref (get field)))


(defn wrap-validation [handler]
  (fn [request]
    (binding [*errors* (atom {})]
      (handler request))))