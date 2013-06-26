(ns finacentric.views.templates
  (:use [clabango.filters :only [context-lookup deftemplatefilter]]
        [clabango.parser :only [string->ast ast->groups]]
        [clabango.tags :exclusions [load-template]]
        [clojure.pprint]))


(def template-path "finacentric/views/templates/")

(defn fetch-template [template]
  (-> 
   (Thread/currentThread)
   (.getContextClassLoader)
   (.getResource (str template-path template))))

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


(deftemplatetag "alias" "endalias" [nodes context]
  {:nodes (butlast (rest nodes))
   :context (assoc context
              (-> nodes first :args first keyword)
              (-> nodes first :args second keyword context))})

(defn extract-block [nodes name]
  (->> nodes
       (filter #(and (= (:type %) :block) (= (:name %) name)))
       first
       :nodes))


(deftemplatetag "include" [nodes context]
  (let [[node] nodes
        [template] (:args node)
        template (second (re-find #"\"(.*)\"" template))]
    {:string (fetch-template template)
     :context context}))

(deftemplatetag "includeblock" [nodes context]
  (let [args (-> nodes first :args strip-quotes)
        [template block] args]
    {:nodes (extract-block (-> template fetch-template string->ast (ast->groups context)) block)
     :context context}))

(deftemplatetag "debug" [nodes context]
  (prn nodes)
  (prn context))


(deftemplatetag "case" [nodes context]
  {:nodes nodes :context context})


(deftemplatetag "switch" "endswitch" [[switch-node & nodes] context]
  (let [args (:args switch-node)
        nodes (butlast nodes)
        _ (assert (= (count args) 1) "Switch should have exactly one arg")
        sym (first args)
        [default nodes] (split-with #(not= (:tag-name %) "case") nodes)
        nodes (loop [[case-node & nodes] nodes res {}]
                (if-not case-node
                  res
                  (let [[body others] (split-with #(not= (:tag-name %) "case") nodes)
                        _ (assert (-> case-node :args count (>= 1))
                                  (str "Empty case"))
                        case-values (->> case-node :args (map read-string))]
                    (assert (every? #(not (contains? res %)) case-values)
                            (str "Doubled case: "
                                 (some #(when (contains? res %) %) case-values)))
                    (recur others (reduce #(assoc % %2 body) res case-values)))))]

        ;; (partition-by #(= (:tag-name %) "case") nodes)
        ;; cases (partition-all 2 nodes)
        ;; _ (assert (every? #(-> % count (= 2))) "Switch error 1")
        ;; _ (assert (every? #(-> % first count (= 1))) "Switch error 2")
        ;; _ (assert (every? #(-> % first first :tag-name (= "case")) "Switch error 3"))
        ;; cases (into (for [[[case-val] body] cases]
        ;;         [(-> case-val :args first read-string)
        ;;          body]))
  {:nodes (get nodes (context-lookup context sym) default)}))



;; (deftemplatefilter "format" [node body arg]
;;   (prn node body arg)
;;   (when body
;;     (format arg body)))

