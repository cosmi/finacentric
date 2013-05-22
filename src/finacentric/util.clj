(ns finacentric.util
  (:require [noir.io :as io]
            [markdown.core :as md]
            [noir.request]))

(defn format-time
  "formats the time using SimpleDateFormat, the default format is
   \"dd MMM, yyyy\" and a custom one can be passed in as the second argument"
  ([time] (format-time time "dd MMM, yyyy"))
  ([time fmt]
    (.format (new java.text.SimpleDateFormat fmt) time)))

(defn md->html
  "reads a markdown file from public/md and returns an HTML string"
  [filename]
  (->>
    (io/slurp-resource filename)
    (md/md-to-html-string)))


(defmacro with-pagination [page-no & body]
  `(let [~page-no (-> noir.request/*request* :params ~(-> page-no name keyword))
         ~page-no (or (when ~page-no (Integer/parseInt ~page-no)) 0)]
     ~@body
     ))

(defmacro with-integer [id & body]
  (if (contains? &env id) ;; if there is a local variable id, then use it, otherwise get one from *request*
    `(let [~id (when ~id (Integer/parseInt ~id))]
       ~@body)
    `(let [~id (-> noir.request/*request* :params ~(-> id name keyword))
           ~id (when ~id (Integer/parseInt ~id))]
       ~@body)))



