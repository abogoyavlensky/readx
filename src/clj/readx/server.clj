(ns readx.server
  (:require [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [muuntaja.core :as muuntaja-core]
            [readx.routes :as app-routes]
            [readx.utils.config :as config]
            [readx.utils.server :as server-utils]
            [reitit.coercion.malli :as coercion-malli]
            [reitit.dev.pretty :as pretty]
            [reitit.ring :as ring]
            [reitit.ring.coercion :as ring-coercion]
            [reitit.ring.middleware.multipart :as ring-multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as ring-parameters]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.content-type :as content-type]
            [ring.middleware.cookies :as ring-cookies]
            [ring.middleware.default-charset :as default-charset]
            [ring.middleware.not-modified :as not-modified]
            [ring.middleware.session :as ring-session]
            [ring.middleware.session.cookie :as ring-session-cookie]
            [ring.middleware.ssl :as ring-ssl]
            [ring.middleware.x-headers :as x-headers]
            [sentry-clj.ring :as sentry-ring])
  (:import com.zaxxer.hikari.HikariDataSource
           java.util.concurrent.ExecutorService))

(defmethod ig/assert-key ::server
  [_ params]
  (config/validate-schema!
    {:component ::server
     :data params
     :schema [:map
              [:options
               [:map
                [:port pos-int?]
                [:session-secret-key string?]
                [:cookie-attrs-secure? boolean?]
                [:auto-reload? boolean?]
                [:allowed-origins [:or [:= :all] [:vector string?]]]
                [:cache-assets? {:optional true} boolean?]
                [:cache-control {:optional true} string?]]]
              [:db [:fn
                    {:error/message "Invalid datasource type"}
                    #(instance? HikariDataSource %)]]
              [:sentry [:enum :sentry-initialized nil]]
              [:pool [:fn
                      {:error/message "Invalid thread pool type"}
                      #(instance? ExecutorService %)]]]}))

(defn ring-handler
  "Return main application handler for server-side rendering."
  [{:keys [options]
    :as context}]
  (let [session-store (ring-session-cookie/cookie-store
                        {:key (server-utils/string->16-byte-array
                                (:session-secret-key options))})]
    (ring/ring-handler
      (ring/router
        (app-routes/api-routes context)
        {:exception pretty/exception
         :data {:muuntaja muuntaja-core/instance
                :coercion coercion-malli/coercion
                :middleware [[server-utils/wrap-cors (:allowed-origins options)]
                             [x-headers/wrap-content-type-options :nosniff]
                             [x-headers/wrap-frame-options :sameorigin]
                             ring-ssl/wrap-hsts
                             server-utils/wrap-xss-protection
                             not-modified/wrap-not-modified
                             content-type/wrap-content-type
                             [default-charset/wrap-default-charset "utf-8"]
                             ; add handler options to request
                             [server-utils/wrap-context context]
                             ; sentry error reporting
                             sentry-ring/wrap-sentry-tracing
                             ; parse request parameters
                             ring-parameters/parameters-middleware
                             ; negotiate request and response
                             muuntaja/format-middleware
                             ; parse multipart bodies (after muuntaja so it
                             ; handles multipart routes instead of muuntaja)
                             ring-multipart/multipart-middleware
                             ; handle exceptions
                             server-utils/exception-middleware
                             ; coerce request and response to spec
                             ring-coercion/coerce-exceptions-middleware
                             ring-coercion/coerce-request-middleware
                             ring-coercion/coerce-response-middleware]}})
      (ring/routes
        (server-utils/create-resource-handler-cached {:path "/assets/"
                                                      :cached? (:cache-assets? options)
                                                      :cache-control (:cache-control options)})
        (server-utils/create-index-handler)
        (ring/redirect-trailing-slash-handler)
        (ring/create-default-handler))
      ; Session middleware at ring-handler level so both router routes
      ; and fallback handlers (index page) share the same session
      {:middleware [ring-cookies/wrap-cookies
                    [ring-session/wrap-session
                     {:cookie-attrs {:secure (:cookie-attrs-secure? options)
                                     :http-only true}
                      :flash true
                      :store session-store}]
                    anti-forgery/wrap-anti-forgery]})))

(defmethod ig/init-key ::server
  [_ {:keys [options]
      :as context}]
  (log/info "[SERVER] Starting server...")
  (let [handler-fn #(ring-handler context)
        handler (if (:auto-reload? options)
                  (server-utils/wrap-reload handler-fn)
                  (handler-fn))]
    (server-utils/set-default-exception-handler!)
    (jetty/run-jetty handler {:port (:port options)
                              :host "0.0.0.0"
                              :join? false})))

(defmethod ig/halt-key! ::server
  [_ server]
  (log/info "[SERVER] Stopping server...")
  (.stop server))
