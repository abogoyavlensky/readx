(ns readx.convert-test
  (:require [clj-http.client :as http]
            [clojure.string :as str]
            [clojure.test :refer :all]
            [readx.test-utils :as utils]
            [readx.utils.epub :as epub]))

(use-fixtures :once
  (utils/with-system))

(use-fixtures :each
  utils/with-truncated-tables)

(deftest test-convert-epub-success
  (let [epub-bytes (epub/create-test-epub "<p>Hello world</p>")
        url (utils/get-server-url (utils/server))
        response (http/post (str url "/api/convert-epub")
                            {:cookies (utils/session-cookies
                                        {utils/CSRF-TOKEN-SESSION-KEY utils/TEST-CSRF-TOKEN})
                             :headers {utils/CSRF-TOKEN-HEADER utils/TEST-CSRF-TOKEN}
                             :multipart [{:part-name "file"
                                          :name "test.epub"
                                          :content (java.io.ByteArrayInputStream. epub-bytes)}]
                             :as :byte-array})
        content (epub/read-chapter-content (:body response))]
    (is (= 200 (:status response)))
    (is (str/includes? (get-in response [:headers "Content-Type"]) "application/epub+zip"))
    (is (str/includes? (get-in response [:headers "Content-Disposition"]) "test-bionic.epub"))
    (is (str/includes? content "<b>Hel</b>lo"))
    (is (str/includes? content "<b>wor</b>ld"))))

(deftest test-convert-epub-no-file
  (let [url (utils/get-server-url (utils/server))
        response (http/post (str url "/api/convert-epub")
                            {:cookies (utils/session-cookies
                                        {utils/CSRF-TOKEN-SESSION-KEY utils/TEST-CSRF-TOKEN})
                             :headers {utils/CSRF-TOKEN-HEADER utils/TEST-CSRF-TOKEN}
                             :throw-exceptions false
                             :as :json})]
    (is (= 400 (:status response)))))

(deftest test-convert-epub-wrong-csrf-token
  (let [url (utils/get-server-url (utils/server))
        response (http/post (str url "/api/convert-epub")
                            {:cookies (utils/session-cookies
                                        {utils/CSRF-TOKEN-SESSION-KEY utils/TEST-CSRF-TOKEN})
                             :headers {utils/CSRF-TOKEN-HEADER "wrong-token"}
                             :throw-exceptions false})]
    (is (= 403 (:status response)))))

(deftest test-convert-epub-unsupported-format
  (let [url (utils/get-server-url (utils/server))
        response (http/post (str url "/api/convert-epub")
                            {:cookies (utils/session-cookies
                                        {utils/CSRF-TOKEN-SESSION-KEY utils/TEST-CSRF-TOKEN})
                             :headers {utils/CSRF-TOKEN-HEADER utils/TEST-CSRF-TOKEN}
                             :multipart [{:part-name "file"
                                          :name "document.txt"
                                          :content (java.io.ByteArrayInputStream. (.getBytes "not an epub"))}]
                             :throw-exceptions false
                             :coerce :always
                             :as :json})]
    (is (= 400 (:status response)))
    (is (= "Only EPUB files are supported" (get-in response [:body :error])))))

(deftest test-convert-epub-file-too-large
  (let [url (utils/get-server-url (utils/server))
        large-bytes (byte-array (+ 10485760 1))
        response (http/post (str url "/api/convert-epub")
                            {:cookies (utils/session-cookies
                                        {utils/CSRF-TOKEN-SESSION-KEY utils/TEST-CSRF-TOKEN})
                             :headers {utils/CSRF-TOKEN-HEADER utils/TEST-CSRF-TOKEN}
                             :multipart [{:part-name "file"
                                          :name "large.epub"
                                          :content (java.io.ByteArrayInputStream. large-bytes)}]
                             :throw-exceptions false
                             :coerce :always
                             :as :json})]
    (is (= 413 (:status response)))
    (is (= "File size exceeds 10MB limit" (get-in response [:body :error])))))
