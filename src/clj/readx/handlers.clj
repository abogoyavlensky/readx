(ns readx.handlers
  (:require [ring.util.response :as response]))

(defn health-handler
  "Health check handler."
  [_]
  (response/response {:status "OK"}))
