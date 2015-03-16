(defproject org.spootnik/sqlstream "0.1.0"
  :description "MySQL binlog to kafka"
  :url "https://github.com/pyr/sqlstream"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :aot :all
  :main org.spootnik.sqlstream
  :dependencies [[org.clojure/clojure                           "1.6.0"]
                 [org.clojure/tools.logging                     "0.3.1"]
                 [org.spootnik/logconfig                        "0.7.3"]
                 [cheshire                                      "5.4.0"]
                 [com.github.shyiko/mysql-binlog-connector-java "0.1.2"]
                 [org.apache.kafka/kafka-clients                "0.8.2.1"]])
