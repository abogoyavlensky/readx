(ns readx.handlers
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [com.climate.claypoole :as cp]
            [readx.utils.epub :as epub]
            [ring.util.response :as response]
            [sentry-clj.metrics :as metrics])
  (:import (java.io ByteArrayInputStream File)))

(def ^:const MAX-FILE-SIZE 10485760) ; 10MB

(defn health-handler
  "Health check handler."
  [_]
  (response/response {:status "OK"}))

(defn convert-epub-handler
  "Receive an EPUB file, convert to bionic reading format, return the result."
  [request]
  (let [start (System/nanoTime)
        file (get-in request [:multipart-params "file"])
        filename (:filename file)
        file-size (cond
                    (instance? File (:tempfile file)) (.length ^File (:tempfile file))
                    (:bytes file) (count (:bytes file))
                    :else 0)]
    (cond
      (or (nil? file) (str/blank? filename))
      (do
        (metrics/increment "epub.conversion" 1.0 nil {:status "validation-error"})
        (metrics/distribution "epub.conversion_duration"
                              (/ (- (System/nanoTime) start) 1e6) :millisecond
                              {:status "validation-error"
                               :reason "missing"})
        (-> (response/response {:error "No file provided"})
            (response/status 400)))

      (not (str/ends-with? (str/lower-case filename) ".epub"))
      (do
        (metrics/increment "epub.conversion" 1.0 nil {:status "validation-error"})
        (metrics/distribution "epub.conversion_duration"
                              (/ (- (System/nanoTime) start) 1e6) :millisecond
                              {:status "validation-error"
                               :reason "invalid-format"})
        (-> (response/response {:error "Only EPUB files are supported"})
            (response/status 400)))

      (> file-size MAX-FILE-SIZE)
      (do
        (metrics/increment "epub.conversion" 1.0 nil {:status "validation-error"})
        (metrics/distribution "epub.conversion_duration"
                              (/ (- (System/nanoTime) start) 1e6) :millisecond
                              {:status "validation-error"
                               :reason "too-large"})
        (-> (response/response {:error "File size exceeds 10MB limit"})
            (response/status 413)))

      :else
      (try
        (let [pool (get-in request [:context :pool])
              input-stream (if (instance? File (:tempfile file))
                             (java.io.FileInputStream. ^File (:tempfile file))
                             (ByteArrayInputStream. (:bytes file)))
              bionic-name (str (str/replace filename #"(?i)\.epub$" "") "-bionic.epub")
              result @(cp/future pool (epub/convert-to-bionic input-stream))]
          (metrics/increment "epub.conversion" 1.0 nil {:status "success"})
          (metrics/distribution "epub.conversion_duration"
                                (/ (- (System/nanoTime) start) 1e6) :millisecond
                                {:status "success"})
          (metrics/distribution "epub.file_size_input" (double file-size) :byte)
          (metrics/distribution "epub.file_size_output" (double (count result)) :byte)
          (-> (response/response (ByteArrayInputStream. result))
              (response/content-type "application/epub+zip")
              (response/header "Content-Disposition" (str "attachment; filename=\"" bionic-name "\""))
              (response/header "Content-Length" (count result))))
        (catch Exception e
          (log/error e "Failed to convert EPUB")
          (metrics/increment "epub.conversion" 1.0 nil {:status "error"})
          (metrics/distribution "epub.conversion_duration"
                                (/ (- (System/nanoTime) start) 1e6) :millisecond
                                {:status "error"})
          (-> (response/response {:error "Failed to convert EPUB file"})
              (response/status 500)))))))
