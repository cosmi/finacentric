(ns finacentric.models.schema
  (:use [lobos.core :only (defcommand migrate)])
  (:require [clojure.java.jdbc :as sql]
            [lobos.migration :as lm]))

(def db-spec
  {:subprotocol "postgresql"
   :subname "//localhost/finacentric"})

(defcommand pending-migrations []
  (lm/pending-migrations db-spec sname))

(defn actualized?
  "checks if there are no pending migrations"
  []
  (empty? (pending-migrations)))

(defn initialized? []
  true)

(def actualize migrate)

