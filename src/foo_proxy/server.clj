(ns foo-proxy.server
  (:require [foo-proxy
             [nio :as nio]
             [proxy :as proxy]]
            [clojure.core.async :as async])
  (:import java.net.InetSocketAddress
           java.nio.ByteBuffer
           [java.nio.channels
            AsynchronousChannelGroup
            AsynchronousServerSocketChannel
            CompletionHandler]
           [java.util.concurrent
            CountDownLatch
            Executors]))

(def executor (Executors/newSingleThreadExecutor))

;; Latch used to control server startup/shutdown during development
(def ^:dynamic *shutdown-latch* nil)

(declare accept read)

(defn- handle-messages
  "Reads a message from a socket channel, forwards it to proxy and produces
  a reply. Rinse and repeat for next message."
  [socket-chan proxy-conn metrics-chan]
  (let [response-writer (fn [bytes]
                          (nio/write
                           socket-chan
                           bytes
                           (nio/completion-handler
                            (completed [_ _ _]
                              ;; Read next message
                              (read socket-chan proxy-conn metrics-chan)))))
        msg-handler     (fn [bytes]
                          ;; Record metrics about request
                          (async/put! metrics-chan (String. bytes))
                          (proxy/forward proxy-conn metrics-chan response-writer bytes))]
    (nio/read socket-chan msg-handler)))

(defn- read
  [socket-chan proxy-conn metrics-chan]
  (handle-messages socket-chan proxy-conn metrics-chan))

(defn- accept-handler
  [socket-chan proxy-conn metrics-chan]
  (nio/completion-handler
   (completed [this chan _]
    ;; accept next connection
    (accept socket-chan proxy-conn metrics-chan)
    ;; start processing messages on this connection
    (read chan proxy-conn metrics-chan))))

(defn- accept
  [socket-chan proxy-conn metrics-chan]
  (.accept socket-chan nil (accept-handler socket-chan proxy-conn metrics-chan)))

(defn- start* [port channel-group proxy-conn metrics-chan]
  "Starts a server using the given async channel group."
  (let [socket-addr (InetSocketAddress. port)
        socket-chan (.bind (AsynchronousServerSocketChannel/open channel-group) socket-addr)]
    (accept socket-chan proxy-conn metrics-chan)))

(defn start [port forward-host forward-port metrics-chan]
  "Ceremony around starting a server that exposes a way of shutting it down.
   Wires up the proxy client and the metrics processor."
  (alter-var-root #'*shutdown-latch* (fn [_] (CountDownLatch. 1)))
  (let [channel-group (AsynchronousChannelGroup/withThreadPool executor)
        proxy-conn    (proxy/connect forward-host forward-port)
        server-thread (start* port channel-group proxy-conn metrics-chan)]
    (try
      (.await *shutdown-latch*)
      (println "Shutting down")
      (.shutdownNow channel-group)
      (catch InterruptedException e
        (println "Interrupted")))))

(defn shutdown []
  (alter-var-root #'*shutdown-latch* (fn [latch] (.countDown latch))))
