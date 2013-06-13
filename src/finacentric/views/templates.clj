(ns finacentric.views.templates
  (:use [clabango.filters :only [context-lookup deftemplatefilter]]
        [clabango.parser :only [string->ast ast->groups]]
        [clabango.tags]
        [clojure.pprint]))

(deftemplatetag "firstof" [[{args :args}] context]
  {:string (some identity (map #(context-lookup context %) args))
   :context context})

(defn single-or-seq [seq]
  (if (= (count seq) 1)
    (first seq)
    seq))

(defn strip-quotes [strings]
  (map #(if (#{\' \"} (first %))
          (subs % 1 (-> % count dec))
          %)
       strings))

(deftemplatetag "let" "endlet" [nodes context]
  {:nodes (butlast (rest nodes))
   :context (assoc context
              (-> nodes first :args first keyword)
              (-> nodes first :args rest strip-quotes single-or-seq))})

(defn extract-block [nodes name]
  (->> nodes
       (filter #(and (= (:type %) :block) (= (:name %) name)))
       first
       :nodes))

(deftemplatetag "includeblock" [nodes context]
  (let [args (-> nodes first :args strip-quotes)
        [template block] args]
    {:nodes (extract-block (-> template load-template string->ast (ast->groups context)) block)
     :context context}))

(deftemplatetag "debug" [nodes context]
  (prn nodes)
  (prn context))



;; (deftemplatefilter "format" [node body arg]
;;   (prn node body arg)
;;   (when body
;;     (format arg body)))

