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
           :ajax {
                    :suppliers "/buyer/ajax/suppliers"
                    :invites "/buyer/ajax/regcodes"
                    
                    }
           :suppliers {:list "/buyer/suppliers/list"
                       :invites "/buyer/suppliers/invites"}
           
           }
   :supplier {
           :dashboard "/supplier/dashboard"
           }

   
           })

(defn get-links-table []
  links-table)

(defn link [& args]
  (get-in links-table args))
