(ns finacentric.validation-utils)


(defn is-nip? [s]
  (re-matches #"[0-9]{10}" s))


(defn is-regon? [s]
  (re-matches #"[0-9]{9}|[0-9]{14}" s))