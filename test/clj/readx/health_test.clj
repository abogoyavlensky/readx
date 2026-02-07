(ns readx.health-test
  (:require [clj-http.client :as http]
            [clojure.test :refer :all]
            [readx.server :as-alias server]
            [readx.test-utils :as utils]))

(use-fixtures :once
  (utils/with-system))

(use-fixtures :each
  utils/with-truncated-tables)

(deftest test-health-endpoint-ok
  (let [url (utils/get-server-url (utils/server))
        response (http/get (str url "/health") {:as :json})]
    (is (= 200 (:status response)))
    (is (= {:status "OK"} (:body response)))))
