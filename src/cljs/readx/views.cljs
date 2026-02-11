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

(def ^:private token-re (js/RegExp. "\\p{L}+|[^\\p{L}]+" "u"))
(def ^:private letter-re (js/RegExp. "^\\p{L}" "u"))

(defn- to-bionic
  "Convert `text` to bionic reading format as hiccup.
  Bolds the first ceil(len/2) characters of each word."
  [text]
  (let [tokens (re-seq token-re text)]
    (into [:<>]
          (map (fn [token]
                 (if (re-find letter-re token)
                   (let [bold-len (js/Math.ceil (/ (count token) 2))]
                     [:<> [:b (subs token 0 bold-len)] (subs token bold-len)])
                   token)))
          tokens)))

; Page sections

(defn- header-section []
  [:header {:class "border-b border-paper-dark"}
   [:div {:class "max-w-5xl mx-auto px-6 py-5 flex items-center justify-between"}
    [:a {:href "/"
         :class "flex items-center gap-3"}
     [icons/logo]
     [:span {:class "font-display text-3xl tracking-tight text-ink"}
      "Read" [:span {:class "text-accent"} "X"]]]
    [:a {:href "https://github.com"
         :target "_blank"
         :rel "noopener noreferrer"
         :class "text-ink-muted hover:text-ink transition-colors"
         :title "GitHub"}
     [icons/github]]]])

(defn- hero-section []
  [:div {:class "max-w-5xl mx-auto px-6 pt-14 pb-2 text-center"}
   [:h1 {:class "font-display text-5xl md:text-6xl text-ink leading-tight"}
    "Make your books" [:br] [:span {:class "text-accent"} "easier to read"]]
   [:p {:class "mt-5 text-ink-light text-xl max-w-2xl mx-auto leading-relaxed"}
    "Upload an EPUB, get it back with bionic reading styling. "
    [:b {:class "text-ink font-semibold"} "Th"] "e "
    [:b {:class "text-ink font-semibold"} "fir"] "st "
    [:b {:class "text-ink font-semibold"} "ha"] "lf "
    [:b {:class "text-ink font-semibold"} "o"] "f "
    [:b {:class "text-ink font-semibold"} "ea"] "ch "
    [:b {:class "text-ink font-semibold"} "wo"] "rd "
    [:b {:class "text-ink font-semibold"} "i"] "s "
    [:b {:class "text-ink font-semibold"} "bol"] "ded "
    [:b {:class "text-ink font-semibold"} "s"] "o "
    [:b {:class "text-ink font-semibold"} "yo"] "ur "
    [:b {:class "text-ink font-semibold"} "ey"] "es "
    [:b {:class "text-ink font-semibold"} "gli"] "de "
    [:b {:class "text-ink font-semibold"} "fas"] "ter."]])

(defn- csrf-token
  "Get CSRF token from meta tag in HTML head."
  []
  (some-> (js/document.querySelector "meta[name='csrf-token']")
          (.getAttribute "content")))

(defn- trigger-download
  "Create a temporary link to download a blob with given `filename`."
  [blob filename]
  (let [url (.createObjectURL js/URL blob)
        a (.createElement js/document "a")]
    (set! (.-href a) url)
    (set! (.-download a) filename)
    (.appendChild (.-body js/document) a)
    (.click a)
    (.removeChild (.-body js/document) a)
    (.revokeObjectURL js/URL url)))

(defn- upload-and-convert
  "POST the `file` to /api/convert-epub, trigger download on success."
  [file convert-state]
  (reset! convert-state :processing)
  (let [form-data (js/FormData.)
        xhr (js/XMLHttpRequest.)
        original-name (.-name file)
        bionic-name (str (str/replace original-name #"\.epub$" "") "-bionic.epub")]
    (.append form-data "file" file)
    (set! (.-responseType xhr) "blob")
    (.addEventListener xhr "load"
                       (fn []
                         (if (<= 200 (.-status xhr) 299)
                           (do
                             (trigger-download (.-response xhr) bionic-name)
                             (reset! convert-state :done))
                           (reset! convert-state :error))))
    (.addEventListener xhr "error"
                       (fn [] (reset! convert-state :error)))
    (.open xhr "POST" "/api/convert-epub")
    (when-let [token (csrf-token)]
      (.setRequestHeader xhr "X-CSRF-Token" token))
    (.send xhr form-data)))

(defn- converter-section []
  (let [selected-file (r/atom nil) ; stores the File object
        convert-state (r/atom :idle) ; :idle :processing :done :error
        drag-over? (r/atom false)
        file-input-ref (r/atom nil)]
    (fn []
      (let [processing? (= @convert-state :processing)]
        [:section {:class "max-w-5xl mx-auto px-6 py-10"}
         [:div {:class "bg-white rounded-2xl shadow-sm border border-paper-dark p-8 md:p-12"}

          ; Upload zone
          [:div {:class (str "upload-zone rounded-xl p-10 text-center transition-all"
                             (if processing?
                               " opacity-60 cursor-not-allowed pointer-events-none"
                               " cursor-pointer")
                             (when @drag-over? " drag-over")
                             (when @selected-file " file-ready"))
                 :on-click #(when-not processing?
                              (when-let [input @file-input-ref] (.click input)))
                 :on-drag-over (fn [e]
                                 (.preventDefault e)
                                 (when-not processing?
                                   (reset! drag-over? true)))
                 :on-drag-leave #(reset! drag-over? false)
                 :on-drop (fn [e]
                            (.preventDefault e)
                            (reset! drag-over? false)
                            (when-not processing?
                              (let [file (-> e .-dataTransfer .-files (aget 0))]
                                (when (and file (str/ends-with? (.-name file) ".epub"))
                                  (reset! selected-file file)
                                  (reset! convert-state :idle)))))}
           [:input {:type "file"
                    :accept ".epub"
                    :class "hidden"
                    :disabled processing?
                    :ref #(reset! file-input-ref %)
                    :on-change (fn [e]
                                 (when-let [file (-> e .-target .-files (aget 0))]
                                   (reset! selected-file file)
                                   (reset! convert-state :idle)))}]

           (if @selected-file
             [:div {:class "fade-in"}
              [icons/file-ready]
              [:p {:class "text-ink font-semibold text-xl"} (.-name @selected-file)]
              [:p {:class "text-ink-muted text-base mt-1"} "Ready to convert"]]
             [:div
              [icons/upload]
              [:p {:class "text-ink-light text-xl font-medium"} "Drop your EPUB here"]
              [:p {:class "text-ink-muted text-base mt-1"} "or click to browse"]])]

          ; Convert button
          [:button {:class (str "btn-convert w-full mt-8 text-white font-semibold text-xl py-5 rounded-xl transition-colors "
                                (case @convert-state
                                  :done "bg-green-700 hover:bg-green-800"
                                  :error "bg-red-700 hover:bg-red-800"
                                  "bg-accent hover:bg-accent-dark")
                                (when (or (nil? @selected-file) processing?)
                                  " opacity-40 cursor-not-allowed"))
                    :disabled (or (nil? @selected-file) processing?)
                    :on-click (fn []
                                (when @selected-file
                                  (upload-and-convert @selected-file convert-state)))}
           (case @convert-state
             :processing "Processing..."
             :done "\u2713 Conversion complete \u2014 download ready"
             :error "Conversion failed \u2014 click to retry"
             "Convert to Bionic EPUB")]]]))))

(def ^:const DEBOUNCE-MS 300)

(defn- convert-text!
  "Convert `text` to bionic format and store in `output` atom."
  [text output]
  (let [trimmed (str/trim text)]
    (if (str/blank? trimmed)
      (reset! output nil)
      (let [lines (str/split-lines trimmed)]
        (reset! output
                (into [:<>]
                      (interpose [:br]
                                 (map #(if (str/blank? %) "" (to-bionic %))
                                      lines))))))))

(defn- demo-section []
  (let [demo-input (r/atom DEFAULT-DEMO-TEXT)
        demo-output (r/atom nil)
        timer (atom nil)]
    (convert-text! @demo-input demo-output)
    (fn []
      [:section {:class "max-w-5xl mx-auto px-6 py-14"}
       [:div {:class "text-center mb-10"}
        [:h2 {:class "font-display text-3xl md:text-4xl text-ink"} "Try it out"]
        [:p {:class "text-ink-muted text-lg mt-3"} "Paste any text below to preview the bionic reading effect."]]

       [:div {:class "grid md:grid-cols-2 gap-8"}
        ; Input
        [:div
         [:label {:class "block text-sm font-semibold text-ink-muted uppercase tracking-wider mb-3"} "Original text"]
         [:textarea {:class "w-full h-80 bg-white border border-paper-dark rounded-xl p-6 text-ink leading-loose resize-none font-body text-xl placeholder:text-ink-muted"
                     :placeholder "Type or paste text here..."
                     :value @demo-input
                     :on-change (fn [e]
                                  (let [text (-> e .-target .-value)]
                                    (reset! demo-input text)
                                    (when @timer (js/clearTimeout @timer))
                                    (reset! timer (js/setTimeout #(convert-text! text demo-output) DEBOUNCE-MS))))}]]
        ; Output
        [:div
         [:label {:class "block text-sm font-semibold text-ink-muted uppercase tracking-wider mb-3"} "Bionic preview"]
         [:div {:class "bionic-result w-full h-80 bg-white border border-paper-dark rounded-xl p-6 leading-loose text-xl overflow-y-auto"}
          (if @demo-output
            @demo-output
            [:span {:class "text-ink-muted italic text-lg"} "Start typing to see the result..."])]]]])))

(defn- footer-section []
  [:footer {:class "border-t border-paper-dark"}
   [:div {:class "max-w-5xl mx-auto px-6 py-6 flex flex-col sm:flex-row items-center justify-between gap-3"}
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
    [:div {:class "max-w-3xl mx-auto px-6"}
     [:div {:class "divider"}]]
    [demo-section]]
   [footer-section]])
