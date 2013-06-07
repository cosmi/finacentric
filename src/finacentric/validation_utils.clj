(ns finacentric.validation-utils)

(defn- is-number-valid? [no weights]
  (when (= (count no) (count weights))
    (let [no (mapv #(-> % str Integer/parseInt) no)
          res (reduce + (map * weights no))]
      (= (mod res 11) (last no)))))

(defn is-nip? [nip]
  (is-number-valid? nip [6 5 7 2 3 4 5 6 7 0]))

(defn is-regon? [regon]
  (case (count regon)
    9 (is-number-valid? regon [8 9 2 3 4 5 6 7 0])
    14 (is-number-valid? regon [2 4 8 5 0 9 7 3 6 1 2 4 8 0])
    false))

