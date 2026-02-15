(ns readx.sentry
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [readx.utils.config :as config]
            [sentry-clj.core :as sentry]))

(defmethod ig/assert-key ::sentry
  [_ params]
  (config/validate-schema!
    {:component ::sentry
     :data params
     :schema [:map
              [:dsn any?]]}))

(defmethod ig/init-key ::sentry
  [_ {:keys [dsn]}]
  (if dsn
    (do
      (log/info "[SENTRY] Initialising Sentry...")
      (sentry/init! dsn {:traces-sample-rate 1.0
                         :logs-enabled true})
      (log/info "[SENTRY] Sentry initialised successfully.")
      :sentry-initialized)
    (log/info "[SENTRY] No Sentry DSN provided.")))

(defmethod ig/halt-key! ::sentry
  [_ status]
  (log/info "[SENTRY] Closing Sentry SDK...")
  (when (= status :sentry-initialized)
    (sentry/close!)))
