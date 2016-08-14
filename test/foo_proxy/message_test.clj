(ns foo-proxy.message-test
  (:require [clojure.test :refer :all]
            [foo-proxy.message :as message]))

(deftest message-test
  (testing "Parsing of message type"
    (is (= :req (message/parse-message-type "REQ 1 Foobar")))
    (is (= :req (message/parse-message-type "REQ 2")))))
