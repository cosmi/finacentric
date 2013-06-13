(ns finacentric.views.layout
  (:use noir.request)
  (:require [clabango.parser :as parser]
            [clabango.tags :as cl-tags]
            [clabango.filters :as cl-filters]
            [finacentric.views.templates :as tags]
            [noir.session :as session]))

(def template-path "finacentric/views/templates/")

(defn render [template & [params]]
  (parser/render-file (str template-path template)
                      (assoc (or params {})
                        :context (:context *request*)
                        :user-id (session/get :user-id))))

(defn render-block [template block & [params]]
  (let [context (assoc (or params {})
                  :context (:context *request*)
                  :user-id (session/get :user-id))]
    (-> (str template-path template)
        cl-tags/load-template
        parser/string->ast
        (parser/ast->groups context)
        (tags/extract-block block)
        parser/groups->parsed
        parser/realize)))

