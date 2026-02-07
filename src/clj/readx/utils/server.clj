(ns readx.utils.server
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [hiccup2.core :as hiccup]
            [manifest-edn.core :as manifest]
            [reitit.core :as reitit]
            [reitit.ring :as ring]
            [reitit.ring.middleware.exception :as exception]
            [ring.middleware.anti-forgery :as anti-forgery]
            [ring.middleware.gzip :as gzip]
            [ring.middleware.x-headers :as x-headers]
            [ring.util.response :as response])
  (:import (java.security MessageDigest)))

; Middlewares

(defn wrap-context
  "Add system dependencies of handler to request as a context key."
  [handler context]
  (fn [request]
    (-> request
        (assoc :context context)
        (handler))))

(defn wrap-reload
  "Reload ring handler on every request. Useful in dev mode."
  [f]
  ; require reloader locally to exclude dev dependency from prod build
  (let [reload! ((requiring-resolve 'ring.middleware.reload/reloader) ["src"] true)]
    (fn
      ([request]
       (reload!)
       ((f) request))
      ([request respond raise]
       (reload!)
       ((f) request respond raise)))))

(defn string->16-byte-array [s]
  (let [digest (MessageDigest/getInstance "MD5")]
    (.digest digest (.getBytes s "UTF-8"))))

; URLs

(defn route
  "Return the API route by its name, with optional path and query parameters."
  ([router route-name]
   (route router route-name {}))
  ([router route-name {:keys [path query]}]
   (-> router
       (reitit/match-by-name route-name path)
       (reitit/match->path query))))

; Exceptions

(defn- get-error-path
  [exception]
  (mapv
    (comp #(str/join ":" %) :at)
    (:via (Throwable->map exception))))

(defn- default-error-handler
  [error-type exception _request]
  {:status 500
   :body {:type error-type
          :path (get-error-path exception)
          :error (ex-data exception)
          :details (ex-message exception)}})

(defn- wrap-exception
  [handler e request]
  (log/error e (pr-str (:request-method request) (:uri request)) (ex-message e))
  (handler e request))

(def exception-middleware
  "Common exception middleware to handle all errors."
  (exception/create-exception-middleware
    (merge
      exception/default-handlers
      {; override the default handler
       ::exception/default (partial default-error-handler "UnexpectedError")

       ; print stack-traces for all exceptions
       ::exception/wrap wrap-exception})))

(defn set-default-exception-handler!
  "Set a default uncaught exception handler that logs error."
  []
  (Thread/setDefaultUncaughtExceptionHandler
    (fn [thread ex]
      (log/error ex "Uncaught exception on" (.getName thread)))))

; Handlers

(defn csrf-token
  "Return the CSRF token value."
  []
  (force anti-forgery/*anti-forgery-token*))

(def ^:private DEFAULT-CACHE-365D "public,max-age=31536000,immutable")

(defn create-resource-handler-cached
  "Return resource handler with optional Cache-Control header."
  [{:keys [cached? cache-control]
    :or {cached? false}
    :as opts}]
  (letfn [(resource-response-cached-fn
            ([path]
             (resource-response-cached-fn path {}))
            ([path options]
             (-> (response/resource-response path options)
                 (response/header "Cache-Control" (or cache-control DEFAULT-CACHE-365D)))))]
    (let [response-fn (if cached?
                        resource-response-cached-fn
                        response/resource-response)]
      (-> response-fn
          (ring/-create-file-or-resource-handler opts)
          (gzip/wrap-gzip)))))

(defn wrap-xss-protection
  ([handler]
   (wrap-xss-protection handler {}))
  ([handler _options]
   (x-headers/wrap-xss-protection handler true nil)))

(defn- index
  "Index HTML page."
  []
  [:html
   {:lang "en"}
   [:head
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=0"}]
    [:meta {:name "msapplication-TileColor"
            :content "#ffffff"}]
    [:link {:rel "manifest"
            :href "/assets/manifest.json"}]
    [:link {:rel "icon"
            :href (manifest/asset "images/icon@32px.png")}]
    [:link {:rel "icon"
            :href (manifest/asset "images/icon.svg")
            :type "image/svg+xml"}]
    [:link {:rel "apple-touch-icon"
            :sizes "180x180"
            :href (manifest/asset "images/icon@180px.png")}]
    [:link {:href (manifest/asset "css/output.css")
            :rel "stylesheet"
            :type "text/css"}]
    [:title "Clojure Stack SPA"]]
   [:body
    [:div {:id "app"}]
    [:script {:src (manifest/asset "js/main.js")
              :type "text/javascript"}]]])

(defn- render-index-html
  "Render index.html page."
  []
  (-> (str "<!DOCTYPE html>\n" (hiccup/html (index)))
      (response/response)
      (response/content-type "text/html")))

(defn create-index-handler
  "Create handler for render index.html on any request."
  []
  (fn
    ([_request]
     (render-index-html))
    ([_request respond _]
     (respond (render-index-html)))))
