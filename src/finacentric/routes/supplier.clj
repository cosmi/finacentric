(ns finacentric.routes.supplier
  (:use [compojure.core]
        [causeway.utils]
        [finacentric.ctrl auth company invoice]
        [finacentric.links]
        [finacentric.render :only [render]]
        [causeway.validation :only [validates? get-errors validate-let]])
  (:require [ring.util.response :as response]))


(defroutes supplier-routes
  (context "/api" []
    (context "/invoice/:id" [id]
      (let-routes [id (Integer/parseInt id)]
        (DELETE "/" []
                (remove-invoice! id)
                )
        (DELETE "/file" []
                (remove-invoice-upload! id)
                ))))

  
  (context "/supplier" [page order dir]
    (context "/invoices" []
      (GET "/new" []
        (render "supplier/invoices/new.form.html" {}))
      (POST "/new" {form :params :as req}
        (prn :REQ req (slurp (req :body)))
        (validate-let [data (validates? upload-invoice-validator form)]
                  (let [invoice (post-invoice! data)]
                    (response/redirect ((link :supplier :invoices :details) (invoice :id))))
                  (render "supplier/invoices/new.form.html" {:error (get-errors) :form form}))
        )

      
      (let-routes [page (Integer/parseInt (or page "0"))]
        (GET "/all" []
          (render "supplier/invoices/+list.html" {:page page :order order :dir dir
                                                :invoices (get-all-invoices-for-supplier page order dir)
                                                })
          )
        (GET "/unverified" []
          (render "supplier/invoices/+list.html" {:page page :order order :dir dir
                                                :invoices (get-unverified-invoices-for-supplier page order dir)
                                                })
          )
        (GET "/accepted" []
          (render "buyer/invoices/accepted.html" {})
          )
        )
      

      )))
