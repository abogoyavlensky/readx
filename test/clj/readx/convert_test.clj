(ns readx.convert-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [readx.handlers :as handlers]
            [readx.utils.epub :as epub]))

(deftest test-convert-epub-success
  (let [epub-bytes (epub/create-test-epub "<p>Hello world</p>")
        request {:multipart-params {"file" {:filename "test.epub"
                                            :bytes epub-bytes}}}
        response (handlers/convert-epub-handler request)
        body-bytes (.readAllBytes (:body response))
        content (epub/read-chapter-content body-bytes)]
    (is (= 200 (:status response)))
    (is (= "application/epub+zip" (get-in response [:headers "Content-Type"])))
    (is (str/includes? (get-in response [:headers "Content-Disposition"]) "test-bionic.epub"))
    (is (str/includes? content "<b>Hel</b>lo"))
    (is (str/includes? content "<b>wor</b>ld"))))

(deftest test-convert-epub-no-file
  (let [response (handlers/convert-epub-handler {:multipart-params {}})]
    (is (= 400 (:status response)))))
