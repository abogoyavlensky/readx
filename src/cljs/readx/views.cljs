(ns readx.views
  (:require [readx.assets :as assets]
            [readx.router :as-alias ui-router]
            [readx.subs :as subs]
            [re-frame.core :as re-frame]
            [reitit.frontend.easy :as reitit-easy]))

(defn- page-not-found
  []
  [:div {:class "min-h-screen flex items-center justify-center bg-gray-50"}
   [:div {:class "text-center"}
    [:h1 {:class "text-8xl font-bold text-gray-800 mb-4"} "404"]
    [:h2 {:class "text-2xl font-semibold text-gray-700 mb-6"} "Page Not Found"]
    [:p {:class "text-gray-600 mb-8"}
     "The page you're looking for doesn't exist."]
    [:a {:href (reitit-easy/href ::ui-router/home)
         :class "inline-block px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"}
     "Back to Home"]]])

(defn router-component
  "Component for routing frontend navigation."
  [{:keys [router]}]
  (let [current-route @(re-frame/subscribe [::subs/current-route])
        view (get-in current-route [:data :view])]
    (if current-route
      [view {:router router
             :current-route current-route}]
      [page-not-found])))

(defn home-view
  "Render home page."
  [_]
  [:div
   {:class ["text-slate-800" "min-h-screen" "flex" "flex-col"]}
   [:main {:class ["flex-grow" "flex" "items-center" "justify-center"]}
    [:div {:class ["container" "mx-auto" "px-4" "max-w-4xl" "text-center"]}
     [:h1 {:class ["text-6xl" "font-bold" "mb-6" "text-slate-900"]} "Welcome to "
      [:span {:class ["bg-gradient-to-r" "from-emerald-400" "to-violet-500" "bg-clip-text" "text-transparent" "relative"]}
       "Clojure Stack SPA"]]
     [:p {:class ["text-2xl" "mb-10" "text-slate-600"]} "A complete, modern template to jumpstart your Clojure/ClojureScript project"]
     [:p {:class ["text-lg" "mb-12" "text-slate-500"]}
      "To begin, modify the existing view in " [:code {:class ["bg-slate-100" "px-1" "rounded"]} "src/cljs/readx/views.cljs"]
      " or add a new API route in " [:code {:class ["bg-slate-100" "px-1" "rounded"]} "src/cljc/readx/routes.cljc"]
      " and define a handler in " [:code {:class ["bg-slate-100" "px-1" "rounded"]} "src/clj/readx/handlers.clj"]]
     [:div {:class ["mb-16"]}
      [:a {:class ["bg-slate-900" "hover:bg-slate-800" "text-white" "font-medium" "py-3" "px-8" "rounded-lg" "transition-colors" "duration-200" "mr-4"]
           :href "https://stack.bogoyavlensky.com/docs/spa/tutorial"
           :target "_blank"} "Get Started"]]]]
   [:footer {:class ["py-6" "text-center" "text-sm" "text-slate-500"]}
    [:p {:class "flex items-center justify-center gap-2"}
     "Made with ❤️ for the "
     [:img {:src (assets/asset "images/clojure_logo.png")
            :alt "Clojure"
            :class "h-6 inline"}]
     "community"]]])
