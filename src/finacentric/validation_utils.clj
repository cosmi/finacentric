(ns finacentric.validation-utils)

(defn is-nip? [nip]
  (when (= (count nip) 10)
    (let [nip (mapv #(-> % str Integer/parseInt) nip)
          res (reduce + (map * [6 5 7 2 3 4 5 6 7 0] nip))]
      (= (mod res 11) (get nip 9)))))

(defn is-regon? [regon]
  (when (= (count regon) 9)
    (let [regon (mapv #(-> % str Integer/parseInt) regon)
          res (reduce + (map * [8 9 2 3 4 5 6 7 0] regon))]
      (= (mod res 11) (get regon 8)))))

