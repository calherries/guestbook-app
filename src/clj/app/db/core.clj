(ns app.db.core
  (:require
   [clojure.java.jdbc :as jdbc]
   [java-time :refer [java-date]]
   [java-time.pre-java8 :as jt]
   [next.jdbc.date-time]
   [next.jdbc.result-set]
   [conman.core :as conman]
   [mount.core :refer [defstate]]
   [app.config :refer [env]]))

(defstate ^:dynamic *db*
          :start (conman/connect! {:jdbc-url (env :database-url)})
          :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

;; (extend-protocol next.jdbc.result-set/ReadableColumn
;;   java.sql.Timestamp
;;   (read-column-by-label [^java.sql.Timestamp v _]
;;     (.toLocalDateTime v))
;;   (read-column-by-index [^java.sql.Timestamp v _2 _3]
;;     (.toLocalDateTime v))
;;   java.sql.Date
;;   (read-column-by-label [^java.sql.Date v _]
;;     (.toLocalDate v))
;;   (read-column-by-index [^java.sql.Date v _2 _3]
;;     (.toLocalDate v))
;;   java.sql.Time
;;   (read-column-by-label [^java.sql.Time v _]
;;     (.toLocalTime v))
;;   (read-column-by-index [^java.sql.Time v _2 _3]
;;     (.toLocalTime v)))

                                        ;
(extend-protocol jdbc/IResultSetReadColumn
  java.sql.Timestamp
  (result-set-read-column [v _2 _3]
    (java-date (.atZone (.toLocalDateTime v) (java.time.ZoneId/systemDefault))))
  java.sql.Date
  (result-set-read-column [v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (result-set-read-column [v _2 _3]
    (.toLocalTime v)))
                                        ;

(extend-protocol jdbc/ISQLValue
  java.util.Date
  (sql-value [v]
    (java.sql.Timestamp. (.getTime v)))
  java.time.LocalTime
  (sql-value [v]
    (jt/sql-time v))
  java.time.LocalDate
  (sql-value [v]
    (jt/sql-date v))
  java.time.LocalDateTime
  (sql-value [v]
    (jt/sql-timestamp v))
  java.time.ZonedDateTime
  (sql-value [v]
    (jt/sql-timestamp v)))

(comment (get-messages))
(comment (save-message! {:name "Bob" :message "Hello, world"}))
(comment (save-message! {:name "Alice" :message "Hello, world"}))
(comment (save-message! {:name "Roxy" :message "Hello, world"}))

;; optional step to get the data into postgres
(comment (->>
           (jbdc/query
             {:connection-uri "jdbc:h2:./app_dev.db"}
             ["select name, message, timestamp from guestbook"])
           (jbdc/insert-multi! *db* :posts)))
