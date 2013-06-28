(ns finacentric.clabango
  (:require [clojure.string :as strings]))

(def ^:private SEP (char 0))




(defn nil-node [s]
  {:type nil
   :text s})
(defn tag-node [s]
  {:type :tag
   :text s})
(defn text-node [s]
  {:type :text
   :text s})
(defn var-node [s]
  {:type :var
   :text s})

(defn split-lines [s]
  (butlast (re-seq #".*?\n|.*" s)))

(defn split-vars [s]
  (let [els (-> s
                (strings/replace "{{" (str SEP "{{" SEP))
                (strings/replace "}}" (str SEP "}}" SEP))
                (strings/split (re-pattern (str SEP))))]
    (loop [input els in-var false output []]
      (if (empty? input) output
          (let [[el & rst] input]
            (case el
                  "{{" (do
                         (assert (not in-var) "Double var start")
                         (recur rst true (conj output (nil-node el))))
                  "}}" (do
                         (assert in-var "Lone var end")
                         (recur rst false (conj output (nil-node el))))
                  (recur rst in-var (conj output (if in-var
                                                   (var-node el)
                                                   (text-node el))))))))))

(defn split-str [s]
  (let [tags
        (-> s
            (strings/replace "{%" (str SEP "{%" SEP))
            (strings/replace "%}" (str SEP "%}" SEP))
            (strings/split (re-pattern (str SEP))))]
    (loop [input tags in-tag false output []]
      (if (empty? input) output
          (let [[el & rst] input]
            (case el
                  "{%" (do
                         (assert (not in-tag) "Double tag start")
                         (recur rst true (conj output (nil-node el))))
                  "%}" (do
                         (assert in-tag "Lone tag end")
                         (recur rst false (conj output (nil-node el))))
                  
                  (recur rst in-tag
                         (if in-tag
                           (conj output (tag-node el))
                           (vec (concat output (split-vars el)))))))))))


(defn enumerate-els
  ([s]
     (enumerate-els s {:line 1 :col 1}))
  ([s init]
     (if-let [s (seq s)]
       (cons (merge (first s) init)
             (let [[col line]
                   (loop [el (:text (first s)) i 0 col (init :col) line (init :line)]
                     (if (= i (count el))
                       [col line]
                       (if (= (.charAt el i) \newline)
                         (recur el (inc i) 1 (inc line))
                         (recur el (inc i) (inc col) line))))]
               (enumerate-els (rest s) {:col col :line line})))
       nil)))

(defn splice-nodes [s f]
  (mapcat f s))


(defn mark-tagname [node]
  (if-not
          (-> node :type (= :tag))
    node
    (let [[_ tagname args] (re-matches #"\s*(\S+)\s*(.*?)\s*" (node :text))]
      (assert tagname)
      (assoc node :tagname tagname :args args)
      )))




(defn split-args [args]
  (let [args (read-string (str \( args \)))]
    args
    ))

(defn sym->path [sym]
  (let [path (-> sym
                name
                (strings/split #"\.")
                (->> (map keyword)))]
    path))


(defn lookup [context path]
  (get-in context path)
  )

  
(defn seq-emitter [nodes]
  
  #(for [n nodes]
     (cond (string? n) n
           (fn? n) (n %)
           :else n
           )))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. #^String text
    (replace "&" "&amp;")
    (replace "<" "&lt;")
    (replace ">" "&gt;")
    (replace "\"" "&quot;")
    (replace "'"  "&#39;")
    (replace "`"  "&#96;")))


(def ^:dynamic *filters* (atom {}))

(defmacro def-filter
  ([nom args & body]
     `(swap! *filters* assoc ~nom
             (fn ~args ~@body)))
  ([nom val]
     `(swap! *filters* assoc ~nom
             ~val)))

(defn var-emitter [node]
  (let [args (strings/split (node :text) #"\|")
        args (map strings/trim args)
        [nom & funs] args
        path (-> nom sym->path)
        funs (for [fun funs]
               (let [[_ nom args] (re-matches #"(\S+)\s*(\S*)" fun)
                     args (read-string (str "(" args ")"))
                     fun (@*filters* nom)]
                 (assert fun)
                 (if (empty? args) fun
                     #(apply fun % args))))
        funs (if (empty? funs)
               [str escape-html]
               funs)
        fltr (apply comp (reverse funs))
        ]
    (assert (not (some nil? funs)))
  #(let [val (lookup % path)]
       (-> val fltr))))

(def-filter "esc" escape-html)
(def-filter "str" str)
(def-filter "safe" identity)
(def-filter "count" count)





(defn take-nodes-until-tag [nodes tagname]
  (let [[before after] (split-with #(-> % :tagname (not= tagname)) nodes)
        [endnode & after] after]

    [before endnode after]))


(defn block-selector [block-start block-end block-compiler]
  (fn [nodes]
    (let [[in-block end-block rst] (take-nodes-until-tag nodes block-end)
          [start-block & in-block] in-block]
      (assert #(-> start-block :tagname (= block-start)))
      (assert #(-> end-block :tagname (= block-end)))
      (cons
       (block-compiler start-block in-block end-block) 
       rst)
    )))
      

(defn compile-seq [nodes blocks]
  (let [[node & rnodes] nodes]
    (when node
      (case (:type node)
        :text  (cons (:text node) (compile-seq rnodes blocks))
        nil (compile-seq rnodes blocks)
        :var (cons (var-emitter node) (compile-seq rnodes blocks))
        :tag (let [bfn (blocks (-> node :tagname))]
                 (cond->
                  (cons node (compile-seq rnodes blocks))
                  bfn bfn))))))




(def ^:dynamic *tags* (atom {}))

(def ^:dynamic *blocks* nil)
(defn get-block [name]
  (-> *blocks* deref (get name)))

(defn save-block! [name fun]
  (swap! *blocks* #(cond-> % (not (contains? % name)) (assoc name fun))))



(defn str->emitter [s & [filename]]
  (let [inp (->> s split-str
      ;; (splice-nodes #(if (= (-> % :type :text))
      ;;                  (map text-node (split-lines (% :text)))
      ;;                  [%]))
                (map mark-tagname)
                enumerate-els
                (map #(assoc % :filename filename)))]
      (-> inp (compile-seq @*tags*)
          seq-emitter)))

(defn add-tag!
  ([from to compile-fn]
     (swap! *tags* assoc from (block-selector from to compile-fn)))
  ([from compile-fn]
     (swap! *tags* assoc from (fn [[node & nodes]] (cons (compile-fn node) nodes)))))

(defmacro def-block-tag [from to args & body]
  `(add-tag! ~from ~to (fn ~args ~@body)))


(defmacro def-single-tag [tagname args & body]
  `(add-tag! ~tagname (fn ~args ~@body)))

(defn is-var-name? [arg]
  (re-matches #"([-\w\.]+)" arg)
  )


(defn var-fn [args]
  (assert (is-var-name? args))
  (let [path (sym->path args)]
    #(lookup % path)))

(defn val-fn [args]
  (cond
   (is-var-name? args)
   (let [path (sym->path args)]
     #(lookup % path))
   :else
   (let [val (read-string args)]
     (assert (not (coll? val)))
     (fn [_] val))))

(def if-ops {"=" =})

(defn get-op-fn [x]
  (assert (if-ops x))
  (if-ops x))
             
  
(defn if-selector [args]
  (if-let [[_ a op b] (re-matches #"(\S+)\s+(=)\s+(\S+)" args)]
    (let [a (val-fn a)
          b (val-fn b)
          op (get-op-fn op)]
      #(op (a %) (b %)))
    (var-fn args)))


(def-block-tag "if" "endif" [if-node inner endif-node]
  (let [[then else-node else] (take-nodes-until-tag inner "else")
        if-sel (if-selector (if-node :args))
        then (seq-emitter then)
        else (seq-emitter else)]
    #(if (if-sel %)
       (then %)
       (else %))))

(def-block-tag "block" "endblock" [start-node inner end-node]
  (let [[word rst] (strings/split (start-node :args) #"\s")]
    (assert (empty? rst))
    (save-block! word (seq-emitter inner))
    (get-block word)))




(def-block-tag "for" "endfor" [for-node inner end-node]
  (let [[_ id source] (re-matches #"(\S+)\s*in\s*(\S+)" (for-node :args))
        _ (assert id (for-node :args))
        path1 (sym->path id)
        path2 (sym->path source)
        inner (seq-emitter inner)]
    #(for [o (lookup % path2)]
       (inner (assoc-in % path1 o))
       )))

(def-block-tag "alias" "endalias" [start-node inner end-node]
  (let [[_ id source] (re-matches #"(\S+)\s*(\S+)" (start-node :args))
        _ (assert id (start-node :args))
        path1 (sym->path id)
        path2 (sym->path source)
        inner (seq-emitter inner)]
    #(let [o (lookup % path2)]
       (inner (assoc-in % path1 o))
       )))

(def-block-tag "switch" "endswitch" [switch-node inner end-node]
  (let [cases
        (loop [inner inner out {} vals [::else]]
          (let [[then case else] (take-nodes-until-tag inner "case")
                i (seq-emitter then)
                out (reduce #(assoc % %2 i) out vals)]
            (if case
              (recur else out (read-string (str \[ (case :args) \])))
              out)))
        path (-> (switch-node :args)
                 sym->path)]
    #(let [v (lookup % path)]
       ((or (get cases v) (get cases ::else)) %))))
    

  


(def ^:dynamic *template-path* "")


(defn fetch-template [template]
  (-> 
   (Thread/currentThread)
   (.getContextClassLoader)
   (.getResource (str *template-path* template))))

(defmacro with-template-root [root & body]
  `(binding [*template-path* ~root]
     ~@body))

(defn read-template [string-or-file]
  (let [[s fileref] (if (string? string-or-file)
                      [string-or-file "UNKNOWN"]
                      [(slurp string-or-file) string-or-file])]
    [s fileref]))

(defn- load-renderer [template]
  (let [[s filename] (->
                      template
                      fetch-template
                      read-template)
        emitter  (str->emitter s filename)]
    (save-block! ::root emitter)))


(defn get-renderer [template]
  (binding [*blocks* (atom {})]
    (load-renderer template)
    (let [emitter (get-block ::root)]
      #(->> % emitter flatten (apply str)))))


(def-single-tag "extends" [node]
  (let [args (node :args)
        filename (second (re-matches #"\"(.*)\"" args))]
    (assert filename)
    (load-renderer filename)))


(def-single-tag "include" [node]
  (let [args (node :args)
        filename (second (re-matches #"\"(.*)\"" args))]
    (assert filename (prn-str "?? " args filename))
    (get-renderer filename)))




(def-single-tag "when" [node]
  (let [args (node :args)
        [_ clause val] (re-matches #"(.*?)\s+then\s+(\S+)" args)
        var-fn (var-emitter {:text val})
        if-clause (if-selector clause)]
    #(when (if-clause %)
       (var-fn %))))



