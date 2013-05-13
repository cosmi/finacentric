(ns lobos.config
  (:use lobos.connectivity)
  (:require [finacentric.models.schema :as schema]))

(open-global schema/db-spec)

