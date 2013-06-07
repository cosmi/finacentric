(ns finacentric.util
  (:use [compojure.core]
        [finacentric.validation])
  (:require [noir.io :as io]
            [markdown.core :as md]
            [noir.request]
            ))

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

(defmacro with-page-size [default page-size & body]
  `(let [~page-size (-> noir.request/*request* :params ~(-> page-size name keyword))
         ~page-size (or (when ~page-size (Integer/parseInt ~page-size)) ~default)]
     ~@body
     ))

(defmacro with-integer [id & body]
  (if (contains? &env id) ;; if there is a local variable id, then use it, otherwise get one from *request*
    `(let [~id (when ~id (Integer/parseInt ~id))]
       ~@body)
    `(let [~id (-> noir.request/*request* :params ~(-> id name keyword))
           ~id (when ~id (Integer/parseInt ~id))]
       ~@body)))




(defmacro id-context [id & body]
  `(context ["/:id", :id #"[0-9]+"]  {{~id :id} :params}
     (with-integer ~id
       (routes ~@body))))

(defn current-url []
  (noir.request/*request* :uri))


(defn current-url-append [s]
  (let [url (current-url)]
    (cond-> url
      (.endsWith url "/") (subs 0 (dec (count url)))
      :always (str "/" s))))


(defmacro routes-when [test & body]
  `(if ~test
     (routes ~@body)
     (constantly nil)))



(defmacro FORM [url render validator action & [redirect-to-url]]
  `(let-routes [render# ~render
                validator# ~validator
                action# ~action]
     (GET ~url []
       (render# nil nil))
     (POST ~url {params# :params :as request#}
       (if-let [obj# (validates? validator# params#)]
         (and (action# obj#)
              (resp/redirect ~(or redirect-to-url ".")))
         (render# params# (get-errors)))
       )))
