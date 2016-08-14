(ns foo-proxy.message
  (:require [clojure.string :as string]))

(defn parse-message-type
  "Parses a message like \"REQ 1 Foobar\" into a message type, e.g. \":req\".
  A more efficient way would be to work on the byte level all the way, the
  string handling is a bit unnecessary."
  [s]
  (-> s
      (string/split #" ")
      first
      .toLowerCase
      keyword))
