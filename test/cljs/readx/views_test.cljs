(ns readx.views-test
  (:require [cljs.test :refer [deftest is testing]]
            [readx.views :as views]))

(def ^:private to-bionic #'views/to-bionic)

(deftest test-home-view-returns-hiccup
  (let [result (views/home-view {})]
    (is (vector? result))
    (is (= :div (first result)))))

(deftest test-to-bionic-single-word
  (is (= [:<> [:<> [:b "Hel"] "lo"]]
         (to-bionic "Hello"))))

(deftest test-to-bionic-short-words
  (testing "one letter word"
    (is (= [:<> [:<> [:b "I"] ""]]
           (to-bionic "I"))))
  (testing "two letter word"
    (is (= [:<> [:<> [:b "i"] "s"]]
           (to-bionic "is")))))

(deftest test-to-bionic-sentence
  (let [result (to-bionic "Hello world")]
    (is (= [:<> [:<> [:b "Hel"] "lo"] " " [:<> [:b "wor"] "ld"]]
           result))))

(deftest test-to-bionic-preserves-punctuation
  (is (= [:<> [:<> [:b "Hel"] "lo"] ", " [:<> [:b "wor"] "ld"] "!"]
         (to-bionic "Hello, world!"))))

(deftest test-to-bionic-cyrillic
  (is (= [:<> [:<> [:b "Чте"] "ние"] " " [:<> [:b "эт"] "о"]]
         (to-bionic "Чтение это"))))

(deftest test-to-bionic-greek
  (is (= [:<> [:<> [:b "Γε"] "ιά"] " " [:<> [:b "σο"] "υ"]]
         (to-bionic "Γειά σου"))))

(deftest test-to-bionic-mixed-alphabets
  (let [result (to-bionic "Hello Мир")]
    (is (= [:<> [:<> [:b "Hel"] "lo"] " " [:<> [:b "Ми"] "р"]]
           result))))

(deftest test-to-bionic-empty-string
  (is (= [:<>] (to-bionic ""))))