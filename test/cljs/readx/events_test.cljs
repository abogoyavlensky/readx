(ns readx.events-test
  (:require [cljs.test :refer [is deftest]]
            [day8.re-frame.test :as rf-test]
            [re-frame.core :as rf]
            [re-frame.db :as rf-db]
            [readx.events :as events]))

(defn- current-route []
  (:current-route @rf-db/app-db))

(deftest test-initialize-db
  (rf-test/run-test-sync
    (rf/dispatch [::events/initialize-db])
    (is (nil? (current-route)))))

(deftest test-navigate
  (rf-test/run-test-sync
    (rf/dispatch [::events/initialize-db])
    (rf/dispatch [::events/navigate {:name :home
                                     :path "/"}])
    (let [route (current-route)]
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
    (let [route (current-route)]
      (is (= :about (:name route))))))
