(ns readx.core
  (:gen-class)
  (:require [readx.utils.config :as config]))

(defn -main
  "Run application system in production."
  []
  (config/run-system {:profile :prod
                      :config-path "config.edn"}))
