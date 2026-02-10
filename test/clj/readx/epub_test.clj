(ns readx.epub-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [readx.utils.epub :as epub]))

; Tests for apply-bionic-to-html (only needs JSoup, no epublib)

(deftest test-apply-bionic-to-html-simple-paragraph
  (let [result (epub/apply-bionic-to-html "<html><body><p>Hello world</p></body></html>")]
    (is (str/includes? result "<b>Hel</b>lo"))
    (is (str/includes? result "<b>wor</b>ld"))))

(deftest test-apply-bionic-to-html-preserves-existing-tags
  (let [result (epub/apply-bionic-to-html "<html><body><p><em>Hello</em> world</p></body></html>")]
    (is (str/includes? result "<b>Hel</b>lo"))
    (is (str/includes? result "<b>wor</b>ld"))
    (is (str/includes? result "<em>"))))

(deftest test-apply-bionic-to-html-empty-body
  (let [result (epub/apply-bionic-to-html "<html><body></body></html>")]
    (is (string? result))))

(deftest test-apply-bionic-to-html-multiple-paragraphs
  (let [result (epub/apply-bionic-to-html "<html><body><p>First</p><p>Second</p></body></html>")]
    (is (str/includes? result "<b>Fir</b>st"))
    (is (str/includes? result "<b>Sec</b>ond"))))

(deftest test-apply-bionic-to-html-nested-elements
  (let [result (epub/apply-bionic-to-html "<html><body><div><p><span>Nested text</span></p></div></body></html>")]
    (is (str/includes? result "<b>Nes</b>ted"))
    (is (str/includes? result "<b>te</b>xt"))))

; Tests for full EPUB conversion (requires epublib)

(deftest test-convert-to-bionic-roundtrip
  (let [epub-bytes (epub/create-test-epub "<p>Hello world</p>")
        result (epub/convert-to-bionic (java.io.ByteArrayInputStream. epub-bytes))
        content (epub/read-chapter-content result)]
    (is (str/includes? content "<b>Hel</b>lo"))
    (is (str/includes? content "<b>wor</b>ld"))))

(deftest test-convert-to-bionic-preserves-structure
  (let [epub-bytes (epub/create-test-epub "<h1>Title</h1><p>Some text here</p>")
        result (epub/convert-to-bionic (java.io.ByteArrayInputStream. epub-bytes))
        content (epub/read-chapter-content result)]
    (is (str/includes? content "<h1>"))
    (is (str/includes? content "<b>Tit</b>le"))
    (is (str/includes? content "<b>So</b>me"))))

(deftest test-convert-to-bionic-produces-valid-epub
  (let [epub-bytes (epub/create-test-epub "<p>Testing EPUB validity</p>")
        result (epub/convert-to-bionic (java.io.ByteArrayInputStream. epub-bytes))
        content (epub/read-chapter-content result)]
    (is (some? content))
    (is (str/includes? content "<b>Test</b>ing"))))
