(ns app.validation
  (:require [struct.core :as st]))

(def message-schema
  [[:name
    st/required
    st/string]
   [:message
    st/required
    st/string
    {:message  "message must be more than 10 characters"
     :validate (fn [msg] (>= (count msg) 10))}]])

(defn validate-message [msg]
  (first (st/validate msg message-schema)))
