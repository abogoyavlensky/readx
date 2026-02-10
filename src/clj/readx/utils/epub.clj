(ns readx.utils.epub
  (:require [clojure.string :as str]
            [readx.utils.bionic :as bionic])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream InputStream)
           (nl.siegmann.epublib.domain Book Resource)
           (nl.siegmann.epublib.epub EpubReader EpubWriter)
           (org.jsoup Jsoup)
           (org.jsoup.nodes Document$OutputSettings$Syntax Element TextNode)))

(defn read-epub
  "Read an EPUB from `input-stream`, return an epublib `Book`."
  ^Book [^InputStream input-stream]
  (.readEpub (EpubReader.) input-stream))

(defn write-epub
  "Write an epublib `Book` to a byte array."
  ^bytes [^Book book]
  (let [baos (ByteArrayOutputStream.)]
    (.write (EpubWriter.) book baos)
    (.toByteArray baos)))

(defn- apply-bionic-to-text-node
  "Replace a text node's content with bionic-formatted HTML."
  [^TextNode text-node]
  (let [text (.getWholeText text-node)]
    (when-not (str/blank? text)
      (let [bionic-html (bionic/to-bionic-text text)]
        (when (not= text bionic-html)
          (.before text-node bionic-html)
          (.remove text-node))))))

(defn apply-bionic-to-html
  "Parse XHTML `content`, walk text nodes in the body, apply bionic formatting.
  Returns the modified XHTML string."
  [^String content]
  (let [doc (Jsoup/parse content)]
    ; Configure for XHTML output
    (-> doc
        (.outputSettings)
        (.syntax Document$OutputSettings$Syntax/xml)
        (.escapeMode org.jsoup.nodes.Entities$EscapeMode/xhtml)
        (.charset "UTF-8"))
    ; Walk all text nodes in body
    (doseq [^Element el (.getAllElements (.body doc))]
      (doseq [node (vec (.childNodes el))]
        (when (instance? TextNode node)
          (apply-bionic-to-text-node node))))
    (.html doc)))

(defn- xhtml-resource?
  "Check if a resource is an XHTML content file."
  [^Resource resource]
  (let [media-type (.getMediaType resource)]
    (and media-type
         (let [media-type-name (.getName media-type)]
           (or (= media-type-name "application/xhtml+xml")
               (= media-type-name "text/html"))))))

(defn convert-to-bionic
  "Convert an EPUB from `input-stream` to bionic reading format.
  Returns the modified EPUB as a byte array."
  [^InputStream input-stream]
  (let [book (read-epub input-stream)
        resources (.getAll (.getResources book))]
    (doseq [^Resource resource resources]
      (when (xhtml-resource? resource)
        (let [content (String. (.getData resource) "UTF-8")
              bionic-content (apply-bionic-to-html content)]
          (.setData resource (.getBytes bionic-content "UTF-8")))))
    (write-epub book)))

; Test helpers

(defn create-test-epub
  "Create a minimal EPUB with given XHTML `content` in a chapter. For tests only."
  [content]
  (let [book (Book.)
        xhtml (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                   "<html xmlns=\"http://www.w3.org/1999/xhtml\">"
                   "<head><title>Test</title></head>"
                   "<body>" content "</body></html>")
        resource (Resource. (.getBytes xhtml "UTF-8") "chapter1.xhtml")]
    (.addSection book "Chapter 1" resource)
    (write-epub book)))

(defn read-chapter-content
  "Read the first XHTML resource content from an EPUB byte array. For tests only."
  [epub-bytes]
  (let [book (read-epub (ByteArrayInputStream. epub-bytes))
        resources (.getAll (.getResources book))]
    (->> resources
         (filter xhtml-resource?)
         first
         .getData
         (String.))))
