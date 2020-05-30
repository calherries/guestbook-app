(ns app.auth
  (:require
   [buddy.hashers :as hashers]
   [clojure.java.jdbc :as jdbc]
   [app.db.core :as db]))

(defn create-user! [login password]
  (db/create-user!* {:login    login
                     :password (hashers/derive password)}))

(defn authenticate-user [login password]
  (let [{hashed :password :as user} (db/get-user-for-auth* {:login login})]
    (when (hashers/check password hashed)
      (dissoc user :password))))

(comment (create-user! "Callum" "password"))
(comment (authenticate-user "Callum" "password"))
(comment (authenticate-user "Callum" "bad password"))

(comment (jdbc/query db/*db* ["SELECT * FROM posts"])) ;; doesn't work

(comment ;; doesn't work
  (jdbc/with-db-transaction [t-conn db/*db*]
    (jdbc/db-set-rollback-only! t-conn)
    (db/save-message!
      t-conn
      {:name "Bob"
       :message "Heloooooo"}
      {:connection t-conn})
    (db/get-messages t-conn {})))
