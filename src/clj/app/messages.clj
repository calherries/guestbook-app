(ns app.messages
  (:require
   [app.db.core :as db]
   [app.validation :refer [validate-message]]))

(defn message-list []
  {:messages (vec (db/get-messages))})

(defn save-message! [message]
  (if-let [errors (validate-message message)]
    (throw (ex-info "Message is invalid"
                    {:app/error-id "validation"
                     :errors       errors}))
    (db/save-message! message)))
