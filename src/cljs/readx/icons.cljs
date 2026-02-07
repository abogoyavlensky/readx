(ns readx.icons)

(defn logo []
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

(defn github []
  [:svg {:class "w-6 h-6"
         :fill "currentColor"
         :viewBox "0 0 24 24"}
   [:path {:d "M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"}]])

(defn upload []
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

(defn file-ready []
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

(defn bolt []
  [:svg {:class "w-5 h-5"
         :fill "none"
         :viewBox "0 0 24 24"
         :stroke "currentColor"
         :stroke-width "2"}
   [:path {:d "M13 10V3L4 14h7v7l9-11h-7z"
           :stroke-linecap "round"
           :stroke-linejoin "round"}]])
