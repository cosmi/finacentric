(ns finacentric.render
  (:require [causeway.templates :as templates]
            [finacentric.links :as links]
            [noir.request :as request]))

(def fn-base {:count count
              :beginsWith #(.beginsWith %1 %2)
              :endsWith #(.endsWith %1 %2)
              :str str
              :clj #(-> % symbol find-var deref)})
              


(defn- parse-dir [path]
  (when path
    (subs path 0 (inc (.lastIndexOf path "/")))))


(defn enrich-data [data]
  (let [path (request/*request* :uri)]
    (merge {:links (links/get-links-table)
            :parent (parse-dir path)
            :path path
            :fn fn-base}
           data)))

  
(defn render [template data]
  (cond (string? template)
        (templates/render template (enrich-data data))
        (fn? template)
        (template (enrich-data data))))
