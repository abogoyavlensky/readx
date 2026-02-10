(ns readx.handlers
  (:require [clojure.string :as str]
            [clojure.tools.logging :as log]
            [readx.utils.epub :as epub]
            [ring.util.response :as response])
  (:import (java.io ByteArrayInputStream)))

(defn health-handler
  "Health check handler."
  [_]
  (response/response {:status "OK"}))

(defn convert-epub-handler
  "Receive an EPUB file, convert to bionic reading format, return the result."
  [request]
  (let [file (get-in request [:multipart-params "file"])]
    (if (or (nil? file) (str/blank? (:filename file)))
      (-> (response/response {:error "No file provided"})
          (response/status 400))
      (try
        (let [input-stream (if (instance? java.io.File (:tempfile file))
                             (java.io.FileInputStream. ^java.io.File (:tempfile file))
                             (ByteArrayInputStream. (:bytes file)))
              original-name (:filename file)
              bionic-name (str (str/replace original-name #"\.epub$" "") "-bionic.epub")
              result (epub/convert-to-bionic input-stream)]
          (-> (response/response (ByteArrayInputStream. result))
              (response/content-type "application/epub+zip")
              (response/header "Content-Disposition" (str "attachment; filename=\"" bionic-name "\""))
              (response/header "Content-Length" (count result))))
        (catch Exception e
          (log/error e "Failed to convert EPUB")
          (-> (response/response {:error "Failed to convert EPUB file"})
              (response/status 500)))))))
