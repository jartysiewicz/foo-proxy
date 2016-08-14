(defproject foo-proxy "0.1.0-SNAPSHOT"
  :description "A metrics-gathering TCP proxy"
  :url "https://github.com/jstaffans/foo-proxy"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/core.async "0.2.385"] ; CSP for Clojure
                 [beckon "0.1.1"]                   ; POSIX signal handling
                                                    ; (a black art on the JVM)
                 ]
  :main foo-proxy.core
  :profiles {:uberjar {:aot :all}
             :dev     {:source-paths ["dev"]
                       :repl-options {:init-ns user}}})
