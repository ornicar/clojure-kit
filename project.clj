(defproject prismic "0.1.0-SNAPSHOT"
  :description "Clojure kit for prismic.io API"
  :url "http://prismic.io"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.9.1"]
                 [cheshire "5.3.1"]
                 [slingshot "0.10.3"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/algo.generic "0.1.2"]]
  :main io.prismic.api)
