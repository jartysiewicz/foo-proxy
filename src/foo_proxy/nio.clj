(ns foo-proxy.nio
  (:import java.nio.channels.CompletionHandler
           java.nio.ByteBuffer)
  (:refer-clojure :exclude [read]))

(def ^:constant MAX_MESSAGE_LENGTH 1024)

;; Helper macro for implementing a completion handler that introduces basic error handling
(defmacro completion-handler
  [completed-fn]
  `(reify java.nio.channels.CompletionHandler
     ~completed-fn
     (~(symbol "failed") [~(symbol "this") ~(symbol "e") ~(symbol "a")]
      (println "Error in completion handler")
      (println ~(symbol "e")))))

(defn end-of-message? [msg]
  (= (last msg) 0x0A))

(declare read)

(defn read-handler [chan callback buffer msg]
  (completion-handler
    (completed [this count _]
      (when (> count 0)
        (let [bytes       (byte-array count)
              _           (.flip buffer)
              _           (.get buffer bytes)
              updated-msg (into msg bytes)]
          (if (end-of-message? updated-msg)
            (callback (byte-array updated-msg))
            (do
              ;; Partial message received -- do another read.
              (.compact buffer)
              (read chan callback buffer updated-msg))))))))

(defn read
  ([chan callback] (read chan callback (ByteBuffer/allocate MAX_MESSAGE_LENGTH) []))
  ([chan callback buffer msg]
   (.read chan buffer nil (read-handler chan callback buffer msg))))

(defn write
  ([chan bytes] (write chan bytes (completion-handler
                                   (completed [_ _ _]))))
  ([chan bytes handler]
   "Writes bytes to channel."
   (let [buffer (ByteBuffer/allocate (count bytes))]
     (.put buffer bytes)
     (.flip buffer)
     (.write chan buffer nil handler))))
