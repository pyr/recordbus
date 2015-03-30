(ns org.spootnik.recordbus.sql
  "Publish SQL replication events on a queue."
  (:import com.github.shyiko.mysql.binlog.BinaryLogClient
           com.github.shyiko.mysql.binlog.BinaryLogClient$EventListener
           com.github.shyiko.mysql.binlog.BinaryLogClient$LifecycleListener
           com.github.shyiko.mysql.binlog.event.Event
           com.github.shyiko.mysql.binlog.event.EventType))

(def event-types
  {EventType/UNKNOWN            :unknown
   EventType/START_V3           :start-v3
   EventType/QUERY              :query
   EventType/STOP               :stop
   EventType/ROTATE             :rotate
   EventType/INTVAR             :intvar
   EventType/LOAD               :load
   EventType/SLAVE              :slave
   EventType/CREATE_FILE        :create-file
   EventType/APPEND_BLOCK       :append-block
   EventType/EXEC_LOAD          :exec-load
   EventType/DELETE_FILE        :delete-file
   EventType/NEW_LOAD           :new-load
   EventType/RAND               :rand
   EventType/USER_VAR           :user-var
   EventType/FORMAT_DESCRIPTION :format-description
   EventType/XID                :xid
   EventType/BEGIN_LOAD_QUERY   :begin-load-query
   EventType/EXECUTE_LOAD_QUERY :execute-load-query
   EventType/TABLE_MAP          :table-map
   EventType/PRE_GA_WRITE_ROWS  :pre-ga-write-rows
   EventType/PRE_GA_UPDATE_ROWS :pre-ga-update-rows
   EventType/PRE_GA_DELETE_ROWS :pre-ga-delete-rows
   EventType/WRITE_ROWS         :write-rows
   EventType/UPDATE_ROWS        :update-rows
   EventType/DELETE_ROWS        :delete-rows
   EventType/INCIDENT           :incident
   EventType/HEARTBEAT          :heartbeat
   EventType/IGNORABLE          :ignorable
   EventType/ROWS_QUERY         :rows-query
   EventType/EXT_WRITE_ROWS     :ext-write-rows
   EventType/EXT_UPDATE_ROWS    :ext-update-rows
   EventType/EXT_DELETE_ROWS    :ext-delete-rows
   EventType/GTID               :gtid
   EventType/ANONYMOUS_GTID     :anonymous-gtid
   EventType/PREVIOUS_GTIDS     :previous-gtids})

(defn bitset-vec
  [^java.util.BitSet s]
  (loop [i   0
         res nil]
    (let [next (.nextSetBit s i)]
      (if (neg? next)
        (vec (reverse res))
        (recur (inc next) (conj res next))))))

(defmulti augment-event-map :type)

(defmethod augment-event-map :format-description
  [{:keys [data] :as event}]
  (assoc event
         :binlog-version (.getBinlogVersion data)
         :server-version (.getServerVersion data)
         :header-length  (.getHeaderLength data)))

(defmethod augment-event-map :gtid
  [{:keys [data] :as event}]
  (assoc event
         :gtid  (.getGtid data)
         :flags (.getFlags data)))

(defmethod augment-event-map :query
  [{:keys [data] :as event}]
  (assoc event
         :sql        (.getSql data)
         :error-code (.getErrorCode data)
         :database   (.getDatabase data)
         :exec-time  (.getExecutionTime data)))

(defmethod augment-event-map :rotate
  [{:keys [data] :as event}]
  (assoc event
         :binlog-filename (.getBinlogFilename data)
         :binlog-position (.getBinlogPosition data)))

(defmethod augment-event-map :rows-query
  [{:keys [data] :as event}]
  (assoc event
         :binlog-filename (.getQuery data)))

(defmethod augment-event-map :table-map
  [{:keys [data] :as event}]
  (assoc event
         :database           (.getDatabase data)
         :table              (.getTable data)
         :column-types       (seq (.getColumnTypes data))
         :column-metadata    (seq (.getColumnMetadata data))
         :column-nullability (bitset-vec (.getColumnNullability data))))

(defmethod augment-event-map :update-rows
  [{:keys [data] :as event}]
  (assoc event
         :cols-old (bitset-vec (.getIncludedColumnsBeforeUpdate data))
         :cols-new (bitset-vec (.getIncludedColumns data))
         :rows     (for [[k v] (.getRows data)] [(mapv str k) (mapv str v)])
         :table-id (.getTableId data)))

(defmethod augment-event-map :write-rows
  [{:keys [data] :as event}]
  (assoc event
         :cols      (bitset-vec (.getIncludedColumns data))
         :rows      (mapv (partial mapv str) (.getRows data))
         :table-id  (.getTableId data)))

(defmethod augment-event-map :delete-rows
  [{:keys [data] :as event}]
  (assoc event
         :cols     (bitset-vec (.getIncludedColumns data))
         :rows     (mapv (partial mapv str) (.getRows data))
         :table-id (.getTableId data)))

(defmethod augment-event-map :xid
  [{:keys [data] :as event}]
  (assoc event
         :xid (.getXid data)))

(defmethod augment-event-map :default
  [event]
  event)

(defn event->map
  [e]
  (let [header (.getHeader e)
        data   (.getData e)
        type   (-> (.getEventType header) event-types)]
    (assoc (augment-event-map {:type type :data data})
           :timestamp (.getTimestamp header)
           :server-id (.getServerId header))))

(defn event-listener
  [callback]
  (reify
    BinaryLogClient$EventListener
    (onEvent [this payload]
      (callback (dissoc (event->map payload) :data)))))

(defn replication-client
  [{:keys [host port user password]} callback]
  (doto (BinaryLogClient. host (int port) nil user password)
    (.setServerId 255)
    (.registerEventListener (event-listener callback))))

(defn connect!
  [client]
  (.connect client))
