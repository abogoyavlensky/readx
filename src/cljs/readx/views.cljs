(ns readx.views
  (:require [clojure.string :as str]
            [re-frame.core :as re-frame]
            [readx.icons :as icons]
            [readx.router :as-alias ui-router]
            [readx.subs :as subs]
            [reagent.core :as r]
            [reitit.frontend.easy :as reitit-easy]))

(def ^:const DEFAULT-DEMO-TEXT
  (str "Reading is the complex cognitive process of decoding symbols to derive meaning. "
       "It is a form of language processing. Success in this process is measured "
       "as reading comprehension. Reading is a means for language acquisition, "
       "communication, and sharing information and ideas."))

(defn- to-bionic
  "Convert `text` to bionic reading format.
  Bolds the first ceil(len/2) characters of each word."
  [text]
  (str/replace text #"\b([A-Za-z\u00C0-\u024F]+)\b"
               (fn [[word]]
                 (let [bold-len (js/Math.ceil (/ (count word) 2))]
                   (str "<b>" (subs word 0 bold-len) "</b>" (subs word bold-len))))))

; Page sections

(defn- header-section []
  [:header {:class "border-b border-paper-dark"}
   [:div {:class "max-w-4xl mx-auto px-6 py-5 flex items-center justify-between"}
    [:a {:href "/"
         :class "flex items-center gap-3"}
     [icons/logo]
     [:span {:class "font-display text-2xl tracking-tight text-ink"}
      "Read" [:span {:class "text-accent"} "X"]]]
    [:a {:href "https://github.com"
         :target "_blank"
         :rel "noopener noreferrer"
         :class "text-ink-muted hover:text-ink transition-colors"
         :title "GitHub"}
     [icons/github]]]])

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
            [icons/file-ready]
            [:p {:class "text-ink font-semibold text-xl"} @selected-file]
            [:p {:class "text-ink-muted text-base mt-1"} "Ready to convert"]]
           [:div
            [icons/upload]
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
                :dangerouslySetInnerHTML {:__html (or @demo-output "<span class='text-ink-muted italic'>Click &quot;Convert&quot; to see the result...</span>")}}]]]

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
         [icons/bolt]
         "Convert Demo Text"]]])))

(defn- footer-section []
  [:footer {:class "border-t border-paper-dark"}
   [:div {:class "max-w-4xl mx-auto px-6 py-6 flex flex-col sm:flex-row items-center justify-between gap-3"}
    [:p {:class "text-ink-muted text-sm"}
     "Built by "
     [:a {:href "https://github.com"
          :target "_blank"
          :rel "noopener noreferrer"
          :class "text-ink-light hover:text-accent transition-colors font-medium"} "Andrey Bogoyavlenskiy"]]
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
