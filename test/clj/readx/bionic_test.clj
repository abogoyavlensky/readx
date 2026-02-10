(ns readx.bionic-test
  (:require [clojure.test :refer :all]
            [readx.utils.bionic :as bionic]))

(deftest test-to-bionic-text-single-word
  (is (= "<b>Hel</b>lo" (bionic/to-bionic-text "Hello"))))

(deftest test-to-bionic-text-short-words
  (testing "one letter word"
    (is (= "<b>I</b>" (bionic/to-bionic-text "I"))))
  (testing "two letter word"
    (is (= "<b>i</b>s" (bionic/to-bionic-text "is")))))

(deftest test-to-bionic-text-sentence
  (is (= "<b>Hel</b>lo <b>wor</b>ld" (bionic/to-bionic-text "Hello world"))))

(deftest test-to-bionic-text-preserves-punctuation
  (is (= "<b>Hel</b>lo, <b>wor</b>ld!" (bionic/to-bionic-text "Hello, world!"))))

(deftest test-to-bionic-text-cyrillic
  (is (= "<b>Чте</b>ние <b>эт</b>о" (bionic/to-bionic-text "Чтение это"))))

(deftest test-to-bionic-text-empty-string
  (is (= "" (bionic/to-bionic-text ""))))

(deftest test-to-bionic-text-multiple-spaces
  (is (= "<b>Hel</b>lo  <b>wor</b>ld" (bionic/to-bionic-text "Hello  world"))))
