(ns foo-proxy.metrics
  (:require [clojure.core.async :as async]))

(def initial-metrics
  {:total 0})

(defn- register-msg [metrics msg]
  (println msg)
  (update metrics :total inc))

(defn start-metrics-processor [msg-chan ctrl-chan]
  "Start metric processor on a lightweight thread. Stops when a :metrics/stop
  message is received on the control channel. Can output current metrics
  on a channel a {:report <channel>} message is received."
  (println "Starting metrics processor")
  (async/go
    (loop [metrics initial-metrics]
      (let [[msg _] (async/alts!! [msg-chan ctrl-chan])]
        (when (not= :metrics/stop msg)
          (if-let [report-chan (:metrics/report msg)]
            (do
              (async/put! report-chan metrics)
              (recur metrics))
            (recur (register-msg metrics msg))))))
    (println "Stopping metrics processor")))

(comment
  (def msg-chan (async/chan))
  (def ctrl-chan (async/chan))

  (start-metrics-processor msg-chan ctrl-chan)

  (async/put! msg-chan "Foo")
  (async/put! ctrl-chan :metrics/stop)

  (def report-chan (async/chan))

  (async/put! ctrl-chan {:report report-chan})
  (async/<!! report-chan)
  )
