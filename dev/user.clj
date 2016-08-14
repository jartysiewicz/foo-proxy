(ns user
  (:require [beckon :refer :all]
            [clojure.core.async :as async]
            [foo-proxy
             [metrics :as metrics]
             [server :as server]]))

(def config {:port 8080})

(def started? (atom false))

(def msg-chan (async/chan 256))
(def report-chan (async/chan))

(defn start []
  (metrics/start-metrics-processor msg-chan)
  (future (server/start (:port config) msg-chan))
  (reset! started? true))

(defn stop []
  (when @started?
    (server/shutdown))
  (async/put! msg-chan :metrics/stop)
  (reset! started? false))

;; Make exceptions thrown on background threads visible in the REPL
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (println "Uncaught exception")
     (println ex))))

#_(reset! (beckon/signal-atom "INT") (fn [] "Signal received"))
