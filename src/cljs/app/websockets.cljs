(ns app.websockets
  (:require-macros [mount.core :refer [defstate]])
  (:require [re-frame.core :as rf]
            [taoensso.sente :as sente]
            [mount.core]))

(defstate socket
  :start (sente/make-channel-socket!
           "/ws"
           js/csrfToken
           {:type           :auto ;; determines that Sente can choose AJAX or WebSockets as the connection method
            :wrap-recv-evs? false})) ;; we don't want to receive all messages wrapped in a :chsk/recv event

(defn send! [message]
  (if-let [send-fn (:send-fn @socket)] ;; in JS you have to de-reference the socket (due to mount)
    (send-fn message)
    (throw (ex-info "Couldn't send message, channel isn't open"
                    {:message message}))))

;; handles messages similarly to the server, but dispatches re-frame events
;; instead of interacting with the database

(defmulti handle-message
  (fn [{:keys [id]} _]
    id))

(defmethod handle-message :message/add
  [_ msg-add-event]
  (rf/dispatch msg-add-event))

(defmethod handle-message :message/creation-errors
  [_ [_ response]]
  (rf/dispatch
    [:form/set-server-errors (:errors response)]))

;; Default handlers

(defmethod handle-message :chsk/handshake
  [{:keys [event]} _]
  (.log js/console "Connection established: " (pr-str event)))

(defmethod handle-message :chsk/state
  [{:keys [event]} _]
  (.log js/console "State changed: " (pr-str event)))

(defmethod handle-message :default
  [{:keys [event]} _]
  (.log js/console "Unknown websocket message: " (pr-str event)))

;; Router
(defn receive-message!
  [{:keys [id event] :as ws-message}]
  (do
    (.log js/console "Event received: " (pr-str event))
    (handle-message ws-message event)))

(defstate channel-router
  :start (sente/start-chsk-router!
           (:ch-recv @socket)
           #'receive-message!))
