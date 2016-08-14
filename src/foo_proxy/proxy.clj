(ns foo-proxy.proxy
  (:import java.net.InetSocketAddress
           [java.nio.channels
            AsynchronousSocketChannel
            CompletionHandler
            ReadPendingException]
           java.nio.ByteBuffer
           java.util.concurrent.CountDownLatch)
  (:require [foo-proxy.nio :as nio]))

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

(defn forward [conn response-fn bytes]
  (nio/write conn bytes)
  (nio/read conn response-fn))
