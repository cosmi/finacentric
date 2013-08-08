(ns finacentric.links)

(def ^:private links-table
  {:login "/login"
   :logout "/logout"})

(defn get-links-table []
  links-table)

(defn link [& args]
  (get-in links-table args))
