(ns finacentric.validation
  (:use compojure.core)
  ;(:use finacentric.util)
  (:require [noir.validation :as vali]))




(def ^{:dynamic true :private true} *errors*)
(def ^{:dynamic true :private true} *input*)
(def ^{:dynamic true :private true} *output*)
(def ^{:dynamic true :private true} *context* [])


(defn set-error! [field text]
  (swap! *errors* assoc-in (conj *context* field) text))

(defn set-value! [field value]
  (swap! *input* assoc-in (conj *context* field) value)
  (swap! *output* assoc-in (conj *context* field) value))


(defn get-input-field [field]
  (get @*input* field))
  

(defmacro rule [field test error-msg]
  `(let [~'_ (get-input-field ~field)]
     (if (binding [*context* (conj @#'*context* ~field)]
           ~test)
       (set-value! ~field ~'_)
       (set-error! ~field ~error-msg))))




(defmacro convert
  ([field test]
  `(let [~'_ (get-input-field ~field)]
     (let [res#
           (binding [*context* (conj @#'*context* ~field)]
             ~test)]
       (set-value! ~field res#))))
  ([field test error-msg]
     `(try
        (convert ~field ~test)
        (catch Exception e (set-error! ~field ~error-msg)))))


(defmacro option [field test error-msg]
 `(do
    (convert ~field (not-empty ~'_))
    (rule ~field (or (nil? ~'_) ~test) ~error-msg)))



(defmacro validator [& rules]
  `(fn [input#]
     (binding [*input* (atom input#)
               *output* (atom {})]
       ~@rules
       @@#'*output*
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

(defmacro errors-validate [error-msg & body]
  `(try
    ~@body
    (catch Exception e (throw (ex-info "" {::validation true ::field (conj *context* field) ::error-msg error-msg})))))

(defmacro try-validate [& body]
  (try
    ~@body
    (catch ExceptionInfo e
      (let [data (ex-data e)]
        (if (data ::validation)
          (set-error! (data ::field) (data ::error-msg))
          (throw e))))))


