(ns finacentric.routes.buyer
  (:use [compojure.core]
        [causeway.utils]
        [finacentric.ctrl auth company invoice]
        [finacentric.links]
        [finacentric.render :only [render]]
        [causeway.validation :only [validates? get-errors validate-let]])
  (:require [ring.util.response :as response]
            [causeway.status :as status]))


(defroutes buyer-routes
  (context "/api" []
    (context "/invite/:id" [id]
      (let-routes [id (Integer/parseInt id)]
        (DELETE "/" []
                (delete-invite! id)
        )

        (GET "/" []

          )))

    (context "/invoice/:id"  [id]
      (let-routes [id (Integer/parseInt id)]

        (POST "/verify" []
          (verify-invoice! id)
          )
        
        (DELETE "/verify" []
          (reject-invoice! id)
                )
      )))


  (context "/buyer" [page order dir]
    (let-routes [page (Integer/parseInt (or page "0"))]
      (context "/suppliers" []
        (GET "/list" []
            (render "buyer/suppliers/list.html"
                    {:page page :order order :dir dir
                     :suppliers (get-current-suppliers-list page order dir)})
          )
        (GET "/invites" []
          (render "buyer/suppliers/invites.html"
                  {:page page :order order :dir dir
                   :invites (get-current-regcodes-list page order dir)})
          )
        (GET "/invite" []
          (render "buyer/suppliers/invite.form.html" {})
          )

        (POST "/invite" {form :params}
          (validate-let [data (validates? invite-supplier-validator form)]
                        (do
                          (create-invite! data)
                          (response/redirect (link :buyer :suppliers :invites)))
                        (render "buyer/suppliers/invite.form.html"
                                {:error (get-errors) :form form})))

        )
      (context "/invoices" []
        (GET "/verify" []
          (render "buyer/invoices/+list.html" {:page page :order order :dir dir
                                                :invoices (get-unverified-invoices-for-buyer page order dir)
                                                })
          )
        (GET "/discount" []
          (render "buyer/invoices/discount.html" {})
          )
        (GET "/accept" []
          (render "buyer/invoices/accept.html" {})
          )
        (GET "/all" []
          (render "buyer/invoices/+list.html" {:page page :order order :dir dir
                                                :invoices (get-all-invoices-for-buyer page order dir)
                                                })
          )
        (GET "/pay" []
          (render "buyer/invoices/pay.html" {})
          )
      ))))

    ;; (context "/ajax" []
    ;;   (context "/suppliers" []
    ;;     (GET "/list" [page sorted]
    ;;       (let [page (Integer/parseInt (or page "1"))]
    ;;         (render "buyer/ajax/table_suppliers_list.html" {:page page :sorted sorted
    ;;                                                         :suppliers (get-current-suppliers-list (dec page) sorted)})
    ;;         ))
    ;;     (GET "/invites" [page sorted]
    ;;       (let [page (Integer/parseInt (or page "1"))]
    ;;         (render "buyer/ajax/table_invites_list.html"{:page page :sorted sorted
    ;;                                                      :regcodes (get-current-regcodes-list (dec page) sorted)})
    ;;         )
    ;;       )
    ;;     )

    ;;   (context "/invoices" []
    ;;     (GET "/verify" [page sorted]
    ;;       (let [page (Integer/parseInt (or page "1"))]
    ;;         (render "buyer/ajax/invoices_to_verify.html" {:page page :sorted sorted
    ;;                                                       :invoices ())
    ;;         ))

    ;;     )))


