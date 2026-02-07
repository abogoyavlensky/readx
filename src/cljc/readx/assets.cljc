(ns readx.assets
  #?(:cljs (:require-macros [readx.assets :refer [inline-manifest]]))
  #?(:clj (:require [clojure.edn :as edn]
                    [clojure.java.io :as io])))

#?(:clj
   (defmacro inline-manifest
     "Reads manifest.edn at compile time and inlines it into CLJS."
     []
     (let [manifest-file (io/file "resources-hashed/manifest.edn")]
       (if (.exists manifest-file)
         (-> manifest-file slurp edn/read-string :assets)
         {}))))

#?(:cljs (goog-define PROD false))

(def ^:const DEFAULT-ASSETS-PREFIX "assets")

#?(:cljs
   (def manifest (inline-manifest)))

#?(:cljs
   (defn asset
     "Returns asset path. In prod, uses fingerprinted path from manifest."
     [asset-file]
     (let [resolved (if PROD
                      (get manifest asset-file asset-file)
                      asset-file)]
       (str "/" DEFAULT-ASSETS-PREFIX "/" resolved))))
