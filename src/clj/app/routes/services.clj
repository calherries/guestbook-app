(ns app.routes.services
  (:require
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.parameters :as parameters]
   [app.middleware.formats :as formats]
   [app.middleware.exception :as exception]
   [ring.util.http-response :refer :all]
   [clojure.java.io :as io]
   [app.messages :as msg]
   [app.auth :as auth]))

(defn service-routes []
  ["/api"
   {:coercion   spec-coercion/coercion
    :muuntaja   formats/instance
    :swagger    {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc  true
        :swagger {:info {:title       "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url    "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/message"
    {:post
     {:parameters
      {:body
       {:name    string?
        :message string?}}

      :responses
      {200
       {:body map?}}

      :handler
      (fn [{{params :body} :parameters}]
        (msg/save-message! params)
        (ok {:status :ok}))}}]

   ["/messages"
    {:get
     (fn [_]
       (ok (msg/message-list)))}]
   ;; {:get
   ;;  {:responses
   ;;   {200 {:body
   ;;         {:messages
   ;;          [{:id        pos-int? ;;            :name      string?
   ;;            :message   string?
   ;;            :timestamp inst?}]}}}
   ;;   :handler
   ;;   (fn [_]
   ;;     (ok (msg/message-list)))}}]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]

   ["/math"
    {:swagger {:tags ["math"]}}

    ["/plus"
     {:get  {:summary    "plus with spec query parameters"
             :parameters {:query {:x int?, :y int?}}
             :responses  {200 {:body {:total pos-int?}}}
             :handler    (fn [{{{:keys [x y]} :query} :parameters}]
                           {:status 200
                            :body   {:total (+ x y)}})}
      :post {:summary    "plus with spec body parameters"
             :parameters {:body {:x int?, :y int?}}
             :responses  {200 {:body {:total pos-int?}}}
             :handler    (fn [{{{:keys [x y]} :body} :parameters}]
                           {:status 200
                            :body   {:total (+ x y)}})}}]]

   ["/files"
    {:swagger {:tags ["files"]}}

    ["/upload"
     {:post {:summary    "upload a file"
             :parameters {:multipart {:file multipart/temp-file-part}}
             :responses  {200 {:body {:name string?, :size int?}}}
             :handler    (fn [{{{:keys [file]} :multipart} :parameters}]
                           {:status 200
                            :body   {:name (:filename file)
                                     :size (:size file)}})}}]

    ["/download"
     {:get {:summary "downloads a file"
            :swagger {:produces ["image/png"]}
            :handler (fn [_]
                       {:status  200
                        :headers {"Content-Type" "image/png"}
                        :body    (-> "public/img/warning_clojure.png"
                                     (io/resource)
                                     (io/input-stream))})}}]]
   ["/login"
    {:post {:summary "login"
            :parameters {:body
                         {:login string?
                          :password string?}}
            :responses
            {200
             {:body
              {:identity
               {:login string?
                :created_at inst?}}}
             401
             {:body
              {:message string?}}}

            :handler
            (fn [{{{:keys [login password]} :body} :parameters
                  session :session}]
              (if-some [user (auth/authenticate-user login password)]
                (->
                  (ok
                    {:identity user})
                  (assoc :session (assoc session :identity user)))
                (unauthorized
                  {:message "Incorrect login or password."})))}}]
   ["/register"
    {:post {:parameters
            {:body
             {:login string?
              :password string?
              :confirm string?}}

            :responses
            {200
             {:body
              {:message string?}}

             400
             {:body
              {:message string?}}

             409
             {:body
              {:message string?}}}

            :handler
            (fn [{{{:keys [login password confirm]} :body} :parameters}]
              (if-not (= password confirm)
                (bad-request
                  {:message
                   "Password and Confirm do not match."})
                (try
                  (auth/create-user! login password)
                  (ok
                    {:message "User registration successful. Please log in."})
                  (catch clojure.lang.ExceptionInfo e
                    ;; meant to catch duplicate user here, but can't due to error with with-db-transation
                    (throw e)))))}}]])
