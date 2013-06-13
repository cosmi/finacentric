(ns finacentric.routes.home
  (:use compojure.core)
  (:require [finacentric.views.layout :as layout]
            [finacentric.util :as util]
            [noir.response :as resp]))

(defroutes home-routes
  (GET "/" [] (resp/redirect "/login")))

