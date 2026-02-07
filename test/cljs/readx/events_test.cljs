(ns readx.events-test
  (:require [cljs.test :refer [is deftest]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [readx.events :as events]
            [readx.subs :as subs]))

(deftest test-initialize-db
  (rf-test/run-test-sync
    (rf/dispatch [::events/initialize-db])
    (is (nil? @(rf/subscribe [::subs/current-route])))))

(deftest test-navigate
  (rf-test/run-test-sync
    (rf/dispatch [::events/initialize-db])
    (rf/dispatch [::events/navigate {:name :home
                                     :path "/"}])
    (let [route @(rf/subscribe [::subs/current-route])]
      (is (some? route))
      (is (= :home (:name route)))
      (is (= "/" (:path route))))))

(deftest test-navigate-updates-route
  (rf-test/run-test-sync
    (rf/dispatch [::events/initialize-db])
    (rf/dispatch [::events/navigate {:name :home
                                     :path "/"}])
    (rf/dispatch [::events/navigate {:name :about
                                     :path "/about"}])
    (let [route @(rf/subscribe [::subs/current-route])]
      (is (= :about (:name route))))))
