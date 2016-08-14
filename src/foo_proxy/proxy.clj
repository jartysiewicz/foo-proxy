(ns foo-proxy.proxy
  (:require [clojure.core.async :as async]
            [foo-proxy
             [metrics :as metrics]
             [nio :as nio]])
  (:import java.net.InetSocketAddress
           java.nio.ByteBuffer
           [java.nio.channels AsynchronousSocketChannel CompletionHandler ReadPendingException]
           java.util.concurrent.CountDownLatch))

(defn- connect* [host port]
  (let [socket-addr (InetSocketAddress. host port)
        latch       (CountDownLatch. 1)
        conn        (doto
                      (AsynchronousSocketChannel/open)
                      (.connect socket-addr nil (nio/completion-handler
                                                 (completed
                                                  [_ _ _]
                                                  (.countDown latch)))))]
    (try
      (.await latch)
      (catch InterruptedException e
        (println "Proxy connection interrupted")))
    conn))

(def connect (memoize connect*))

(defn forward [conn metrics-chan response-fn bytes]
  (nio/write conn bytes)
  (nio/read conn (fn [bytes]
                   ;; Record metrics about response.
                   (async/put! metrics-chan (String. bytes))
                   (response-fn bytes))))
