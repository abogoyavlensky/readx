(ns readx.worker
  (:require [clojure.tools.logging :as log]
            [com.climate.claypoole :as cp]
            [integrant.core :as ig]
            [readx.utils.config :as config]))

(defmethod ig/assert-key ::pool
  [_ params]
  (config/validate-schema!
    {:component ::pool
     :data params
     :schema [:map
              [:size pos-int?]]}))

(defmethod ig/init-key ::pool
  [_ {:keys [size]}]
  (log/info "[WORKER] Starting worker pool with" size "threads...")
  (cp/threadpool size {:name "epub-worker"}))

(defmethod ig/halt-key! ::pool
  [_ pool]
  (log/info "[WORKER] Stopping worker pool...")
  (cp/shutdown pool))
