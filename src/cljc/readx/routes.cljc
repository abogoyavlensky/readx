(ns readx.routes
  (:require #?(:clj [readx.handlers :as handlers])
            #?(:cljs [reitit.coercion.malli :as reitit-malli])
            #?(:cljs [reitit.core :as reitit])
            #?(:cljs [reitit.frontend :as reitit-front])))

(defn api-routes
  "API routes with handlers and schema."
  [_context]
  [["/health" {:name ::health
               :get {#?@(:clj [:handler handlers/health-handler])}}]
   ["/api/convert-epub" {:name ::convert-epub
                         :parameters {:multipart :any}
                         #?@(:clj [:post {:handler handlers/convert-epub-handler}])}]])

#?(:cljs
   (def ^:private api-router
     "Fake router of backend api for using in frontend."
     (reitit-front/router
       [(api-routes {})]
       {:data {:coercion reitit-malli/coercion}})))

#?(:cljs
   (defn route
     "Return api route path by its name for using on frontend."
     ([route-name]
      (route route-name {}))
     ([route-name {:keys [path query]}]
      (-> api-router
          (reitit/match-by-name route-name path)
          (reitit/match->path query)))))
