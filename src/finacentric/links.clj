(ns finacentric.links)

(def ^:private links-table
  {:login "/login"
   :logout "/logout"
   :dashboard "/dashboard"
   :register "/register"
   :landing-page "/"
   :reset-password "/forgot-password"
   :register-company "/register-company"
   :register-buyer "/register-buyer"})

(defn get-links-table []
  links-table)

(defn link [& args]
  (get-in links-table args))
