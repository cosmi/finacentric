(ns finacentric.model.utils
  (:use korma.core))


(defn page [query page-no per-page]
  (-> query
      (offset (* page-no per-page))
      (limit per-page)))

