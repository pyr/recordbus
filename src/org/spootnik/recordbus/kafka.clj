(ns org.spootnik.recordbus.kafka
  "Very thin wrapper around kafka producer code"
  (:import org.apache.kafka.clients.producer.Producer
           org.apache.kafka.clients.producer.KafkaProducer
           org.apache.kafka.clients.producer.ProducerRecord
           java.nio.ByteBuffer))

(def key-serializer
  "org.apache.kafka.common.serialization.ByteArraySerializer")

(def value-serializer
  "org.apache.kafka.common.serialization.StringSerializer")

(defn producer
  [^java.util.Properties props]
  (.setProperty props "key.serializer" key-serializer)
  (.setProperty props "value.serializer" value-serializer)
  (KafkaProducer. props))

(defn record
  [^String topic ^Long key ^String payload]
  (let [key-data (-> (ByteBuffer/allocate 8) (.putLong key) (.array))]
    (ProducerRecord. topic key-data payload)))

(defn publish
  [^KafkaProducer producer ^ProducerRecord record]
  (.send producer record))
