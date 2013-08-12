(ns finacentric.links)

(def ^:private links-table
  {:login "/login"
   :logout "/logout"
   :dashboard "/dashboard"
   :register "/register"
   :landing-page "/"
   :reset-password "/forgot-password"
   :register-supplier "/register-supplier"
   :register-buyer "/register-buyer"
   :buyer {:add-supplier "/buyer/add-supplier"
           :dashboard "/buyer/dashboard"
           :invoice-details (fn [& {:keys [invoice]}]
                              (format "/buyer/invoice/%d" invoice))
           :tables {
                    :suppliers-list "/buyer/ajax/suppliers-list"
                    :regcode-list "/buyer/ajax/regcode-list"
                    
                    }
           }
   :supplier {
           :dashboard "/supplier/dashboard"
           }

   
           })

(defn get-links-table []
  links-table)

(defn link [& args]
  (get-in links-table args))
