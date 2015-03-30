(ns org.spootnik.recordbus.config
  (:import java.util.Properties
           java.io.FileInputStream))

(defn read-props
  [path]
  (let [props (doto (Properties.) (.load (FileInputStream. path)))
        mysql {:host     (.remove props "mysql.host")
               :port     (Long/parseLong (.remove props "mysql.port"))
               :user     (.remove props "mysql.user")
               :password (.remove props "mysql.password")}
        topic (or (.remove props "topic") "recordbus")]
    {:sql mysql :kafka props :topic topic}))
