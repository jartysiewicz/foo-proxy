(ns user
  (:require [foo-proxy.server :as server]
            [beckon :refer :all]))

(def config {:port 8080})

(def started? (atom false))

(defn start []
  (future (server/start (:port config)))
  (reset! started? true))

(defn stop []
  (when @started?
    (server/shutdown))
  (reset! started? false))

;; Make exceptions thrown on background threads visible in the REPL
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (println "Uncaught exception")
     (println ex))))

#_(reset! (beckon/signal-atom "INT") (fn [] "Signal received"))
