(ns readx.core
  (:require [readx.events :as events]
            [readx.router :as router]
            [readx.subs]  ; import for cljs compiler
            [readx.views :as views]
            [re-frame.core :as re-frame]
            [reagent.dom.client :as reagent]))

(defonce ^:private ROOT
  (reagent/create-root (.getElementById js/document "app")))

(defn render!
  "Render the page with initializing routes."
  []
  (re-frame/clear-subscription-cache!)
  (router/init-routes!)
  (reagent/render
    ROOT
    [views/router-component {:router router/router}]))

(defn init!
  "Render the whole app with default db value."
  []
  (re-frame/dispatch-sync [::events/initialize-db])
  (render!))
