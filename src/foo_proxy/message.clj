(ns foo-proxy.message
  (:require [clojure.string :as string]))

(defn parse-message-type
  "Parses a message like \"REQ 1 Foobar\" into a message type, e.g. \":req\"."
  [bytes]
  (-> (String. bytes)
      (subs 0 3)
      .toLowerCase
      keyword))
