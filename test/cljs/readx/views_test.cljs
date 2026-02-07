(ns readx.views-test
  (:require [readx.views :as views]
            [cljs.test :refer [deftest is]]
            [clojure.string :as str]))

(deftest test-home-view-returns-hiccup
  (let [result (views/home-view {})]
    (is (vector? result))
    (is (= :div (first result)))))

(deftest test-home-view-contains-heading
  (let [result (views/home-view {})
        html-str (str result)]
    (is (str/includes? html-str "Welcome"))))