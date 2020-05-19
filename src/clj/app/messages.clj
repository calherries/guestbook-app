(ns app.messages
  (:require
   [app.db.core :as db]))

(defn message-list []
  {:messages (vec (db/get-messages))})
