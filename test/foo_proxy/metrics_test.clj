(ns foo-proxy.metrics-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async]
            [foo-proxy.metrics :as metrics]))

;; let tests share channels. Processor is started and shut down for each test.

(def ctrl-chan (async/chan))
(def msg-chan (async/chan))

(defn start-stop-fixture [f]
  (metrics/start-metrics-processor msg-chan ctrl-chan)
  (f)
  (async/put! ctrl-chan :metrics/stop))

(use-fixtures :each start-stop-fixture)

(defn demand-report-on [report-chan]
  (async/put! ctrl-chan {:metrics/report report-chan}))

(deftest metrics-test
  (testing "Total amount of events"
    (let [report-chan (async/chan)]
      (demand-report-on report-chan)
      (is (= 0 (:total (async/<!! report-chan))))

      (async/put! msg-chan "REQ 1 Test")
      (demand-report-on report-chan)
      (is (= 1 (:total (async/<!! report-chan))))

      (doseq [_ (range 10)]
        (async/put! ctrl-chan "REQ X Test"))
      (demand-report-on report-chan)
      (is (= 11 (:total (async/<!! report-chan)))))))
