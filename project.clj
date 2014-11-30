(defproject wrestler "2.0.0"
  :description "A clojure library to wrap REST interfaces for REST clients."
  :url "http://github.com/jbethune/wrestler/"
  :license {:name "New BSD License (3-clause)"
            :url "./LICENSE"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-http "1.0.1"]
                 [org.clojure/data.json "0.2.5"]]
  :plugins [[codox "0.8.10"]]
  :codox {
          :project {:name "wRESTler", :version "2.0.0"}
          :defaults {:doc "FIXME: write docs"}}
)
