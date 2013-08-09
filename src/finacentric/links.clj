(ns finacentric.links)

(def ^:private links-table
  {:login "/login"
   :logout "/logout"
   :dashboard "/dashboard"
   :register-company "/register-company"})

(defn get-links-table []
  links-table)

(defn link [& args]
  (get-in links-table args))
