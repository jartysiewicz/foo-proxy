(ns foo-proxy.core
  (:require [foo-proxy.server :as server]
            [beckon :refer :all]
            [clojure.core.async :as async]
            [foo-proxy.metrics :as metrics])
  (:gen-class))

(def msg-chan (async/chan 1024))
(def report-chan (async/chan))

(defn dump-metrics [report]
  (println
   (apply
    (partial format (str "msg_total=%d msg_req=%d msg_ack=%d msg_nak=%d "
                   "request_rate_1s=%.3f request_rate_10s=%.3f "
                   "response_rate_1s=%.3f response_rate_10s=%.3f"))
    ((juxt :total :req :ack :nak :response-rate-1s :response-rate-10s :request-rate-1s :request-rate-10s) report))))

(defn start-metrics-logger []
  (async/go
    (loop []
      (let [report (async/<! report-chan)]
        (dump-metrics report)
        (recur)))))

(defn request-metrics []
  (async/put! msg-chan {:metrics/report report-chan}))

(defn -main [& args]
  ;; Make exceptions thrown on background threads visible.
  (Thread/setDefaultUncaughtExceptionHandler
   (reify Thread$UncaughtExceptionHandler
     (uncaughtException [_ thread ex]
       (println "Uncaught exception")
       (println ex))))

  (let [port                        (or (some-> (System/getProperty "listen") Integer/parseInt)
                                        8002)
        forward                     (or (some-> (System/getProperty "forward"))
                                        "localhost:8001")
        [forward-host forward-port] (clojure.string/split forward #":")
        forward-port                (Integer/parseInt forward-port)]
    (metrics/start-metrics-processor msg-chan)
    (start-metrics-logger)
    (reset! (beckon/signal-atom "USR2") #{request-metrics})

    (println (str "Listening on port " port))
    (println (str "Forwarding requests to " forward))

    (server/start port forward-host forward-port msg-chan)))
