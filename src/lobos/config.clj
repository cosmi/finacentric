(ns lobos.config
  (:use lobos.connectivity))

(open-global {:subprotocol "postgresql"
              :subname "//localhost/finacentric"})

