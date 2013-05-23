(ns finacentric.ajax
  (:use compojure.core)
  (:require [clojure.data.json :as json])
  
  )

(defn redirect [url]
   [{:method "redirect" :url url}])

(defmacro JSON [path args & body]
  `(POST ~path ~args {:body (json/write-str (do ~@body))
                :status 200
                :headers {"Content-Type" "application/json; charset=utf-8"}}))