(ns readx.router
  (:require [readx.events :as events]
            [readx.views :as views]
            [re-frame.core :as re-frame]
            [reitit.coercion.malli :as reitit-malli]
            [reitit.frontend :as reitit-front]
            [reitit.frontend.easy :as reitit-easy]))

(def ^:private routes
  ["/"
   [""
    {:name ::home
     :view views/home-view}]])

(def router
  "Router for frontend pages."
  (reitit-front/router
    routes
    {:data {:coercion reitit-malli/coercion}}))

(defn- on-navigate
  [new-match]
  (when new-match
    (re-frame/dispatch [::events/navigate new-match])))

(defn init-routes!
  "Initial setup router."
  []
  (reitit-easy/start! router on-navigate {:use-fragment false}))
