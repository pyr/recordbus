(defproject org.spootnik/recordbus "0.1.1"
  :description "MySQL binlog to kafka"
  :url "https://github.com/pyr/recordbus"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot :all
  :main org.spootnik.recordbus
  :dependencies [[org.clojure/clojure                           "1.7.0"]
                 [org.clojure/tools.logging                     "0.3.1"]
                 [spootnik/unilog                               "0.7.8"]
                 [cheshire                                      "5.5.0"]
                 [com.github.shyiko/mysql-binlog-connector-java "0.2.0"]
                 [org.apache.kafka/kafka-clients                "0.8.2.1"]])
