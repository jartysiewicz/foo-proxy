(ns foo-proxy.metrics-test
  (:require [clojure.test :refer :all]
            [clojure.core.async :as async]
            [foo-proxy.metrics :as metrics]))

(defn demand-report [msg-chan report-chan]
  (async/put! msg-chan {:metrics/report report-chan}))

(defn stop-processor [msg-chan]
  (async/put! msg-chan :metrics/stop))

(deftest metrics-total-test
  (let [msg-chan    (async/chan)
        report-chan (async/chan)]
    (metrics/start-metrics-processor msg-chan)

    (demand-report msg-chan report-chan)
    (is (= 0 (:total (async/<!! report-chan))))

    (async/put! msg-chan "REQ 1 Test")
    (demand-report msg-chan report-chan)
    (is (= 1 (:total (async/<!! report-chan))))

    (doseq [_ (range 10)]
      (async/put! msg-chan "REQ X Test"))
    (doseq [_ (range 10)]
      (async/put! msg-chan "ACK X Test"))

    (demand-report msg-chan report-chan)
    (is (= 21 (:total (async/<!! report-chan))))

    (stop-processor msg-chan)))

(deftest metrics-subtotal-test
  (let [msg-chan    (async/chan)
        report-chan (async/chan)]
    (metrics/start-metrics-processor msg-chan)

    (doseq [_ (range 2)]
      (async/put! msg-chan "REQ X Test"))
    (doseq [_ (range 3)]
      (async/put! msg-chan "ACK X Test"))
    (doseq [_ (range 4)]
      (async/put! msg-chan "NAK X Test"))

    (demand-report msg-chan report-chan)
    (let [report (async/<!! report-chan)]
      (is (= 2 (:req report)))
      (is (= 3 (:ack report)))
      (is (= 4 (:nak report)))
      (is (= 9 (:total report))))

    (stop-processor msg-chan)))

(deftest metrics-window-test
  (let [msg-chan    (async/chan)
        report-chan (async/chan)]
    (metrics/start-metrics-processor msg-chan)

    (doseq [_ (range 20)]
      (async/put! msg-chan "REQ X Test"))

    (doseq [_ (range 5)]
      (async/put! msg-chan "ACK X Test")
      (async/put! msg-chan "NAK X Test"))

    (Thread/sleep 500)

    (demand-report msg-chan report-chan)
    (let [{:keys [request-rate-1s response-rate-1s]} (async/<!! report-chan)]
      (is (= 20.0 request-rate-1s))
      (is (= 10.0 response-rate-1s)))

    (Thread/sleep 1000)

    (demand-report msg-chan report-chan)
    (let [{:keys [request-rate-1s response-rate-1s request-rate-10s response-rate-10s]} (async/<!! report-chan)]
      (is (= 0.0 request-rate-1s))
      (is (= 0.0 response-rate-1s))
      (is (= 2.0 request-rate-10s))
      (is (= 1.0 response-rate-10s)))

    (stop-processor msg-chan)))
