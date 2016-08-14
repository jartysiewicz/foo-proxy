(ns foo-proxy.core
  (:require [foo-proxy.server :as server]
            [beckon :refer :all])
  (:gen-class))

(defn -main [& args]
  (future (server/start 8080))
  #_(reset! (beckon/signal-atom "USR2") #{(fn [] (println "Signal received"))})
  (Thread/sleep 60000))
