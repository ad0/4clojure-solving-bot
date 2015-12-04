(defproject a4clojurebot "0.1.0-SNAPSHOT"
  :description "A bot that solves 4clojure problems"
  :url "http://github.com/ad0/4clojure-solving-bot"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clj-http "2.0.0"]]
  :main ^:skip-aot a4clojurebot.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
