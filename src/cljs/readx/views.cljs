(ns readx.views
  (:require [clojure.string :as str]
            [re-frame.core :as re-frame]
            [readx.router :as-alias ui-router]
            [readx.subs :as subs]
            [reagent.core :as r]
            [reitit.frontend.easy :as reitit-easy]))

(def ^:const DEFAULT-DEMO-TEXT
  "Reading is the complex cognitive process of decoding symbols to derive meaning. It is a form of language processing. Success in this process is measured as reading comprehension. Reading is a means for language acquisition, communication, and sharing information and ideas.")

(defn- to-bionic
  "Convert `text` to bionic reading format.
  Bolds the first ceil(len/2) characters of each word."
  [text]
  (str/replace text #"\b([A-Za-z\u00C0-\u024F]+)\b"
               (fn [[word]]
                 (let [bold-len (js/Math.ceil (/ (count word) 2))]
                   (str "<b>" (subs word 0 bold-len) "</b>" (subs word bold-len))))))

; SVG icon components

(defn- logo-icon []
  [:svg {:class "w-8 h-8 text-accent"
         :viewBox "0 0 32 32"
         :fill "none"
         :xmlns "http://www.w3.org/2000/svg"}
   [:path {:d "M6 4C6 2.89543 6.89543 2 8 2H20L26 8V28C26 29.1046 25.1046 30 24 30H8C6.89543 30 6 29.1046 6 28V4Z"
           :fill "currentColor"
           :opacity "0.15"}]
   [:path {:d "M8 2H20L26 8V28C26 29.1046 25.1046 30 24 30H8C6.89543 30 6 29.1046 6 28V4C6 2.89543 6.89543 2 8 2Z"
           :stroke "currentColor"
           :stroke-width "1.8"}]
   [:path {:d "M20 2V8H26"
           :stroke "currentColor"
           :stroke-width "1.8"}]
   [:line {:x1 "10"
           :y1 "14"
           :x2 "22"
           :y2 "14"
           :stroke "currentColor"
           :stroke-width "2.2"
           :stroke-linecap "round"}]
   [:line {:x1 "10"
           :y1 "18.5"
           :x2 "19"
           :y2 "18.5"
           :stroke "currentColor"
           :stroke-width "1.4"
           :opacity "0.45"
           :stroke-linecap "round"}]
   [:line {:x1 "10"
           :y1 "22"
           :x2 "22"
           :y2 "22"
           :stroke "currentColor"
           :stroke-width "2.2"
           :stroke-linecap "round"}]
   [:line {:x1 "10"
           :y1 "26"
           :x2 "16"
           :y2 "26"
           :stroke "currentColor"
           :stroke-width "1.4"
           :opacity "0.45"
           :stroke-linecap "round"}]])

(defn- github-icon []
  [:svg {:class "w-6 h-6"
         :fill "currentColor"
         :viewBox "0 0 24 24"}
   [:path {:d "M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"}]])

(defn- upload-icon []
  [:svg {:class "w-12 h-12 mx-auto text-ink-muted mb-4"
         :fill "none"
         :viewBox "0 0 48 48"
         :stroke "currentColor"
         :stroke-width "1.5"}
   [:path {:d "M14 30L24 20L34 30"
           :stroke-linecap "round"
           :stroke-linejoin "round"}]
   [:path {:d "M24 20V42"
           :stroke-linecap "round"}]
   [:path {:d "M40 30V38C40 40.2091 38.2091 42 36 42H12C9.79086 42 8 40.2091 8 38V30"
           :stroke-linecap "round"}]
   [:rect {:x "16"
           :y "6"
           :width "16"
           :height "4"
           :rx "1"
           :fill "currentColor"
           :opacity "0.2"}]])

(defn- file-ready-icon []
  [:svg {:class "w-12 h-12 mx-auto text-accent mb-4"
         :fill "none"
         :viewBox "0 0 48 48"}
   [:rect {:x "12"
           :y "4"
           :width "24"
           :height "40"
           :rx "3"
           :stroke "currentColor"
           :stroke-width "1.5"
           :fill "currentColor"
           :fill-opacity "0.08"}]
   [:path {:d "M18 16h12M18 22h12M18 28h8"
           :stroke "currentColor"
           :stroke-width "1.5"
           :stroke-linecap "round"}]
   [:circle {:cx "36"
             :cy "36"
             :r "9"
             :fill "#E8F5E9"
             :stroke "#4CAF50"
             :stroke-width "1.5"}]
   [:path {:d "M32 36L35 39L40 33"
           :stroke "#4CAF50"
           :stroke-width "1.8"
           :stroke-linecap "round"
           :stroke-linejoin "round"
           :fill "none"}]])

(defn- bolt-icon []
  [:svg {:class "w-5 h-5"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :stroke-width "2"}
   [:path {:d "M13 10V3L4 14h7v7l9-11h-7z"
           :stroke-linecap "round"
           :stroke-linejoin "round"}]])

; Page sections

(defn- header-section []
  [:header {:class "border-b border-paper-dark"}
   [:div {:class "max-w-4xl mx-auto px-6 py-5 flex items-center justify-between"}
    [:a {:href "/"
         :class "flex items-center gap-3"}
     [logo-icon]
     [:span {:class "font-display text-2xl tracking-tight text-ink"}
      "Read" [:span {:class "text-accent"} "X"]]]
    [:a {:href "https://github.com"
         :target "_blank"
         :rel "noopener noreferrer"
         :class "text-ink-muted hover:text-ink transition-colors"
         :title "GitHub"}
     [github-icon]]]])

(defn- hero-section []
  [:div {:class "max-w-4xl mx-auto px-6 pt-14 pb-2 text-center"}
   [:h1 {:class "font-display text-5xl md:text-6xl text-ink leading-tight"}
    "Make your books" [:br] [:span {:class "text-accent"} "easier to read"]]
   [:p {:class "mt-5 text-ink-light text-xl max-w-2xl mx-auto leading-relaxed"}
    "Upload an EPUB, get it back with bionic reading styling. The "
    [:b {:class "text-ink font-semibold"} "fir"] "st half of ea"
    [:b {:class "text-ink font-semibold"} "ch"] " wo"
    [:b {:class "text-ink font-semibold"} "rd"] " is bold"
    [:b {:class "text-ink font-semibold"} "ed"] " so your ey"
    [:b {:class "text-ink font-semibold"} "es"] " glide fast"
    [:b {:class "text-ink font-semibold"} "er"] "."]])

(defn- converter-section []
  (let [selected-file (r/atom nil)
        convert-state (r/atom :idle) ; :idle :processing :done
        drag-over? (r/atom false)
        file-input-ref (r/atom nil)]
    (fn []
      [:section {:class "max-w-4xl mx-auto px-6 py-10"}
       [:div {:class "bg-white rounded-2xl shadow-sm border border-paper-dark p-8 md:p-12"}

        ; Upload zone
        [:div {:class (str "upload-zone rounded-xl p-10 text-center cursor-pointer transition-all"
                           (when @drag-over? " drag-over")
                           (when @selected-file " file-ready"))
               :on-click #(when-let [input @file-input-ref] (.click input))
               :on-drag-over (fn [e]
                               (.preventDefault e)
                               (reset! drag-over? true))
               :on-drag-leave #(reset! drag-over? false)
               :on-drop (fn [e]
                          (.preventDefault e)
                          (reset! drag-over? false)
                          (let [file (-> e .-dataTransfer .-files (aget 0))]
                            (when (and file (str/ends-with? (.-name file) ".epub"))
                              (reset! selected-file (.-name file))
                              (reset! convert-state :idle))))}
         [:input {:type "file"
                  :accept ".epub"
                  :class "hidden"
                  :ref #(reset! file-input-ref %)
                  :on-change (fn [e]
                               (when-let [file (-> e .-target .-files (aget 0))]
                                 (reset! selected-file (.-name file))
                                 (reset! convert-state :idle)))}]

         (if @selected-file
           [:div {:class "fade-in"}
            [file-ready-icon]
            [:p {:class "text-ink font-semibold text-xl"} @selected-file]
            [:p {:class "text-ink-muted text-base mt-1"} "Ready to convert"]]
           [:div
            [upload-icon]
            [:p {:class "text-ink-light text-xl font-medium"} "Drop your EPUB here"]
            [:p {:class "text-ink-muted text-base mt-1"} "or click to browse"]])]

        ; Convert button
        [:button {:class (str "btn-convert w-full mt-8 text-white font-semibold text-xl py-5 rounded-xl transition-colors "
                              (case @convert-state
                                :done "bg-green-700 hover:bg-green-800"
                                "bg-accent hover:bg-accent-dark")
                              (when (or (nil? @selected-file) (= @convert-state :processing))
                                " opacity-40 cursor-not-allowed"))
                  :disabled (or (nil? @selected-file) (= @convert-state :processing))
                  :on-click (fn []
                              (reset! convert-state :processing)
                              (js/setTimeout #(reset! convert-state :done) 1500))}
         (case @convert-state
           :processing "Processing..."
           :done "\u2713 Conversion complete \u2014 download ready"
           "Convert to Bionic EPUB")]

        [:p {:class "text-center text-ink-muted text-sm mt-4"}
         "Your file is processed locally and never uploaded to any server."]]])))

(defn- demo-section []
  (let [demo-input (r/atom DEFAULT-DEMO-TEXT)
        demo-output (r/atom nil)]
    (fn []
      [:section {:class "max-w-4xl mx-auto px-6 py-14"}
       [:div {:class "text-center mb-10"}
        [:h2 {:class "font-display text-3xl md:text-4xl text-ink"} "Try it out"]
        [:p {:class "text-ink-muted text-lg mt-3"} "Paste any text below to preview the bionic reading effect."]]

       [:div {:class "grid md:grid-cols-2 gap-8"}
        ; Input
        [:div
         [:label {:class "block text-sm font-semibold text-ink-muted uppercase tracking-wider mb-3"} "Original text"]
         [:textarea {:class "w-full h-72 bg-white border border-paper-dark rounded-xl p-6 text-ink leading-relaxed resize-none font-body text-lg placeholder:text-ink-muted"
                     :placeholder "Type or paste text here..."
                     :value @demo-input
                     :on-change #(reset! demo-input (-> % .-target .-value))}]]
        ; Output
        [:div
         [:label {:class "block text-sm font-semibold text-ink-muted uppercase tracking-wider mb-3"} "Bionic preview"]
         [:div {:class "bionic-result w-full h-72 bg-white border border-paper-dark rounded-xl p-6 leading-relaxed text-lg overflow-y-auto"
                :dangerouslySetInnerHTML (when @demo-output {:__html @demo-output})}
          (when-not @demo-output
            [:span {:class "text-ink-muted italic"} "Click \"Convert\" to see the result..."])]]]

       [:div {:class "text-center mt-8"}
        [:button {:class "inline-flex items-center gap-2.5 bg-ink text-paper font-semibold px-10 py-4 rounded-xl hover:bg-ink-light transition-colors text-base"
                  :on-click (fn []
                              (let [text (str/trim @demo-input)]
                                (if (str/blank? text)
                                  (reset! demo-output nil)
                                  (reset! demo-output
                                          (->> (str/split-lines text)
                                               (map #(if (str/blank? %) "" (to-bionic %)))
                                               (str/join "<br>"))))))}
         [bolt-icon]
         "Convert Demo Text"]]])))

(defn- footer-section []
  [:footer {:class "border-t border-paper-dark"}
   [:div {:class "max-w-4xl mx-auto px-6 py-6 flex flex-col sm:flex-row items-center justify-between gap-3"}
    [:p {:class "text-ink-muted text-sm"}
     "Built by "
     [:a {:href "https://github.com"
          :target "_blank"
          :rel "noopener noreferrer"
          :class "text-ink-light hover:text-accent transition-colors font-medium"} "Andrew"]]
    [:p {:class "text-ink-muted text-sm"}
     "ReadX \u2014 Bionic reading for your EPUB library"]]])

; Pages

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
  [:div {:class "font-body text-ink min-h-screen flex flex-col"}
   [header-section]
   [:main {:class "flex-1"}
    [hero-section]
    [converter-section]
    [:div {:class "max-w-2xl mx-auto px-6"}
     [:div {:class "divider"}]]
    [demo-section]]
   [footer-section]])
