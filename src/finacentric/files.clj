(ns finacentric.files
  (:require [monger.gridfs :as gfs]
            monger.joda-time
            [clj-time.core :as time]
            [monger.conversion :as conversion])
  (:use [monger.core :only [connect! connect set-db! get-db]])
  (:import org.bson.types.ObjectId)
  (:import com.mongodb.DBObject))

(def ^:private initialized (atom false))

(defn- init! []
  (reset! initialized #(when-not %
                         (connect!)
                         (set-db! (get-db "finacentric-test"))
                         true)))

(defn store-file! [file-path content-type meta]
  (init!)
  (let [file (gfs/store-file (gfs/make-input-file file-path) (gfs/metadata meta)
                         (gfs/content-type content-type))]
    (-> (file :_id) str)))

(defn input-stream [^DBObject attachment]
  (.getInputStream attachment))

(defn to-map [^DBObject attachment]
  (conversion/from-db-object attachment true))

(defn metadata [^DBObject attachment]
  ((to-map attachment) :metadata))

(defn get-file [file-id]
  (init!)
  (gfs/find-one (ObjectId. file-id)))

