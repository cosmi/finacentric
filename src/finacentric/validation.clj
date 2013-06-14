(ns finacentric.validation
  (:use compojure.core)
  (:import clojure.lang.ExceptionInfo
           java.text.SimpleDateFormat)
  (:import java.sql.Date java.util.GregorianCalendar)
  (:require [noir.validation :as vali]))




(def ^{:dynamic true :private true} *errors*)
(def ^{:dynamic true :private true} *input*)
(def ^{:dynamic true :private true} *output*)
(def ^{:dynamic true :private true} *context* [])


(defn set-error! [field text]
  ;; todo : jak już jest błąd, to nie zmieniaj
  (swap! *errors* assoc-in (conj *context* field) text))

(defn set-value! [field value]
  (swap! *input* assoc-in (conj *context* field) value)
  (swap! *output* assoc-in (conj *context* field) value))


(defn get-input-field [field]
  (get @*input* field))
  

(defmacro rule [field test error-msg]
  `(let [field# ~field]
     (let [~'_ (get-input-field field#)]
       (if (binding [*context* (conj @#'*context* field#)]
             ~test)
         (set-value! field# ~'_)
         (set-error! field# ~error-msg)))))




(defmacro convert
  ([field test]
  `(let [field# ~field]
     (let [~'_ (get-input-field field#)
           res#
           (binding [*context* (conj @#'*context* field#)]
             ~test)]
       (set-value! field# res#))))
  ([field test error-msg]
     `(let [field# ~field]
        (try
        (convert field# ~test)
        (catch Exception e# (set-error! field# ~error-msg))))))


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

(defn has-errors? []
  (not (empty? (get-errors))))

(defn get-field [field]
  (-> *input* deref (get field)))


(defn wrap-validation [handler]
  (fn [request]
    (binding [*errors* (atom {})]
      (handler request))))

(defmacro errors-validate [field error-msg & body]
  `(try
     ~@body
     (catch Exception e#
       (.printStackTrace e#)
       
       (set-error! ~field ~error-msg)
       (throw (ex-info "" {::validation true})))))

(defmacro on-error [& body]
  (throw (Exception. "Lone on-error clause.")))


(defn validation-error [field error-msg]
  (set-error! field error-msg)
  (throw (ex-info "" {::validation true})))


(defmacro try-validate [& body]
  (let [else (last body)
        body (butlast body)]
    (assert (-> else seq?))
    (assert (-> else first name (= "on-error")) "On error clause should be wrapped in (on-error ...)")
    
    `(try
       ~@body
       (catch clojure.lang.ExceptionInfo e#
         (let [data# (ex-data e#)]
           (if (data# ::validation)
             (do
               ~@(rest else))
             (throw e#)))))))




(defn integer-field [field error-msg]
  (convert field (Integer/parseInt _) error-msg))

(defn decimal-field [field scale error-msg-format error-msg-scale]
  (convert field (bigdec (clojure.string/replace _ #"," ".")) error-msg-format)
  (when-not (has-errors?) (rule field (-> _ .scale (<= scale)) error-msg-scale))
  (when-not (has-errors?) (convert field (.setScale _ scale))))

(defn make-sql-date [year month day]
  (java.sql.Date. 
   (.getTimeInMillis 
    (java.util.GregorianCalendar. year month day))))

(defn parse-date [date-str]
  (let [[_ & values] (re-matches #"([0-9]{4})-([0-9]{2})-([0-9]{2})" date-str)
        [year month day] (map #(Integer/parseInt %) values)]
    (make-sql-date year month day)))

(defn date-field [field error-msg-format]
    (convert field (parse-date _) error-msg-format))

