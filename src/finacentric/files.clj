(ns finacentric.files
  (:require [monger.gridfs :as gfs]
            monger.joda-time
            [clj-time.core :as time]
            [monger.conversion :as conversion])
  (:use [monger.core :only [connect! connect set-db! get-db]))
  (:import org.bson.types.ObjectId))

(def ^:private initialized (atom false))

(defn- init! []
  (reset! initialized #(when-not %
                         (connect!)
                         (set-db! (get-db! "finacentric-test"))
                         true)))

(defn- store-file! [file content-type meta]
  (init!)
  (let [file (gfs/store-file file (gfs/metadata meta)
                         (gfs/content-type conten-type-str))]
    (-> (file :_id) str)))

(defn store-invoice-attachment [invoice-id user-id file content-type]
  (init!)
  (store-file! file content-type {:invoice-id invoice-id
                                  :user-id user-id}))

(defn input-stream [attachment]
  (.getInputStream attachment))

(defn to-map [attachment]
  (conversion/fromp-db-object attachment))

(defn metadata [attachment]
  ((to-map attachment) :metadata))

(defn get-invoice-attachment [attachment-id]
  (init!)
  (gfs/find-one (ObjectId. file-id)))

