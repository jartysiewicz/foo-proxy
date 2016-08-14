(ns foo-proxy.metrics
  (:require [clojure.core.async :as async]
            [foo-proxy.message :as message])
  (:import java.util.concurrent.ConcurrentLinkedDeque))

(defn current-time
  []
  (System/currentTimeMillis))

(defn initial-metrics
  []
  {:req    0
   :ack    0
   :nak    0
   :total  0
   ;; Sliding window metrics are kept in a deque, which can grow infinitely
   ;; and cause out-of-memory exceptions. We could have a worker thread clean
   ;; it up periodically.
   :window (ConcurrentLinkedDeque.)})

(defn- register-msg [metrics msg]
  (let [msg-type (message/parse-message-type msg)
        t        (current-time)]
    (-> metrics
        (update msg-type inc)
        (update :total inc)
        (update :window #(doto % (.addFirst {:type msg-type :t t}))))))

(defn- window [metrics msg-type-pred seconds]
  "Produce a sliding window metric on number of messages/second."
  (let [t-now            (current-time)
        msg-of-type?     (fn [{:keys [type]}] (msg-type-pred type))
        msg-in-window?   (fn [{:keys [t]}]
                           (< (- t-now t) (* seconds 1000)))
        num-msgs-of-type (->> metrics
                              :window
                              (filter msg-of-type?)
                              (take-while msg-in-window?)
                              count)]
    (/ num-msgs-of-type seconds)))

(defn- metrics-report [metrics]
  "Produce a report in the form of a hash map."
  (-> metrics
      (select-keys [:req :ack :nak :total])
      (assoc :request-rate-1s (window metrics #(= % :req) 1.0))
      (assoc :response-rate-1s (window metrics #(not= % :req) 1.0))
      (assoc :request-rate-10s (window metrics #(= % :req) 10.0))
      (assoc :response-rate-10s (window metrics #(not= % :req) 10.0))))

(defn start-metrics-processor [msg-chan]
  "Start metrics processor on a lightweight thread. Stops when a :metrics/stop
  message is received on the message channel. Can output current metrics
  on a channel if a {:metrics/report <channel>} message is received."
  (println "Starting metrics processor")
  (async/go
    (loop [metrics (initial-metrics)]
      (let [msg (async/<! msg-chan)]
        (when (not= :metrics/stop msg)
          (if-let [report-chan (:metrics/report msg)]
            (do
              (async/put! report-chan (metrics-report metrics))
              (recur metrics))
            (recur (register-msg metrics msg))))))
    (println "Stopping metrics processor")))
