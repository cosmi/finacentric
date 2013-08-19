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
   :buyer {;; :add-supplier "/buyer/add-supplier"
           ;; :dashboard "/buyer/dashboard"
           ;; :invoice-details (fn [& {:keys [invoice]}]
           ;;                    (format "/buyer/invoice/%d" invoice))
           ;; ajax {
            ;;         :suppliers {:root "/buyer/ajax/suppliers"
            ;;                     :list "/buyer/ajax/suppliers/list"
            ;;                     :invites "/buyer/ajax/suppliers/invites"
            ;;                     }
            ;;         :invoices {:root "/buyer/ajax/invoices"
            ;;                    :all "/buyer/ajax/invoices/all"
            ;;                    :verify "/buyer/ajax/invoices/verify"
            ;;                    :discount "/buyer/ajax/invoices/discount"
            ;;                    :accept "/buyer/ajax/invoices/accept"
            ;;                    :pay "/buyer/ajax/invoices/pay"
            ;;                    }
                    
            ;;         }
           :suppliers {:list "/buyer/suppliers/list"
                       :invites "/buyer/suppliers/invites"
                       :invite "/buyer/suppliers/invite"
                       :root "/buyer/suppliers"
                       :default "/buyer/suppliers/list"
                       :details #(str "/buyer/suppliers/details/" %)}
           
           :invoices {:root "/buyer/invoices"
                      :default "/buyer/invoices/all"
                      :all "/buyer/invoices/all"
                      :verify "/buyer/invoices/verify"
                      :discount "/buyer/invoices/discount"
                      :accept "/buyer/invoices/accept"
                      :pay "/buyer/invoices/pay"
                      }
           :default "/buyer/invoices/list"
           }
   :supplier {
              :default "/supplier/invoices/all" 
              :invoices {:root "/supplier/invoices"
                         :default "/supplier/invoices/all"
                         :all "/supplier/invoices/all"
                         :unverified "/supplier/invoices/unverified"
                         :new "/supplier/invoices/new"
                         :details #(str "/supplier/invoices/details/" %)
                         ;; :discount "/supplier/invoices/discount"
                         ;; :accept "/supplier/invoices/accept"
                         ;; :pay "/supplier/invoices/pay"
                         }
              }
   :partial {
             :default "/register-supplier"
             }
   :api {
         :invite { :delete #(str "/api/invite/" %)}
         :invoice {:delete #(str "/api/invoice/" % )
                   :file {:delete #(str "/api/invoice/" % "/file")
                          :get #(str "/api/invoice/" % "/file") }
                   :verify #(str "/api/invoice/" % "/verify")
                   :unverify #(str "/api/invoice/" % "/verify")
                   }
                   

         }

   
           })

(defn get-links-table []
  links-table)

(defn link [& args]
  (get-in links-table args))
