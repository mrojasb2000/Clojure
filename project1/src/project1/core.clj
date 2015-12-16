(ns project1.core
  (:require [project1.handlers :as handlers]
            [ring.middleware.resource :as resource]
            [ring.middleware.file-info :as file-info]
            [ring.middleware.params]
            [ring.middleware.keyword-params]
            [ring.middleware.multipart-params]
            [ring.middleware.cookies]
            [ring.middleware.session]
            [ring.middleware.session.memory]
            [project1.another :as another]
            [project1.html :as html]
            [clojure.string]))


(defn on-init []
  (println "Initializing sample webapp!"))

(defn on-destroy []
  (println "Destroying sample webapp!"))





;; Content HTML
(defn layout [contents]
  (html/emit
   [:html
    [:body
     [:h1 "Clojure webapps example"]
      [:p "This content comes from layout function"]
      contents]]))



(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

;;(defn example-handler [request]     ;;request map
;;  {:body "Hello Clojure reload!"})  ;;response map

;;(defn example-handler [request]     ;;request map
;;  {:body (pr-str request)})         ;;response map


;; Redirect request
(defn example-handler [request]     ;;request map
  {:headers {"location" "http://github.com/ring-clojure/ring"
             "set-cookie" "test=1"}
   :status 301}
)  ;;response map


;;(defn example-handler [{:keys [uri] :as req}]     ;;request map
;;  {:body (str "URI is: " uri)}                   ;;response map
;;)


(defn case-middleware [handler request]
  (let [request (update-in request [:uri] clojure.string/lower-case)
        response (handler request)]
    (if (string? (:body response))
      (update-in response [:body] clojure.string/capitalize)
      response)))

(defn wrap-case-middleware [handler]
  (fn [request] (case-middleware handler request)))


(defn exception-middleware-fn [handler request]
  (try (handler request)
    (catch Throwable e
      {:status 500 :body (apply str (interpose "\n" (.getStackTrace e)))})))


(defn wrap-exception-middleware [handler]
  (fn [request]
    (exception-middleware-fn handler request)))


(defn not-found-middleware [handler]
  (fn [request]
    (or (handler request)
        {:status 404 :body (str "404 Not Found (with middleware!):" (:uri request))})))

(defn simple-log-middleware [handler]
  (fn [{:keys [uri] :as request}]
    (println "Request path:" uri)
    (handler request)))





(defn test1-handler [request]
  (throw (RuntimeException. "Error!"))
  {:body "test1"})

(defn test2-handler [request]
  {:status 301 :headers {"location" "http://github.com/ring-clojure"}})


;; Form params
;;(defn form-handler [request]
;;  {:status 200
;;   :headers {"Content-type" "text/plain"}
;;   :body (str "local path:\n" (.getAbsolutePath (get-in request [:params :file :tempfile]))
;;              "\nmultiplart-params:\n" (:multipart-params request)
;;              "params:\n" (:params request)
;;              "\nquery-params:\n" (:query-params request)
;;              "\nform-params:\n" (:form-params request))})

(defn cookie-handle [request]
  {:body (layout [:div [:p "Cookies:"]
                       [:pre (:cookies request)]])})

(defn session-handle [request]
  {:body (layout [:div [:p "Session:"]
                       [:pre (:session request)]])})

(defn logout-handle [request]
  {:body "Logged out."
   :session nil})

(defn form-handler [request]
  {:status 200
   :header { "Content-type" "text/html" }
   :cookies {:username (:login (:params request))}  ;;Add cookies data
   :session {:username (:login (:params request))
             :cnt (inc (or (:cnt (:session request)) 0))} ;;Add session data
   :body (layout
          [:div
           [:p "params:"]
           [:pre (:params request)]
           [:p "Query string params:"]
           [:pre (:query-params request)]
           [:p "Form params:"]
           [:pre (:form-params request)]
           [:p "Multipart params:"]
           [:pre (:multipart-params request)]
           [:p "Local path:"]
           [:b (when-let [f (get-in request [:params :file :tempfile])]
                (.getAbsolutePath f))]])})



;; Route definition
(defn route-handler [request]
  (condp = (:uri request)
        "/test1" (test1-handler request)
        "/test2" (test2-handler request)
        "/test3" (handlers/handler3 request)
        "/test4" (another/handler4 request)
        "/form" (form-handler request)  ;; Route form
        "/cookies" (cookie-handle request)
        "/sessions" (session-handle request)
        "/logout" (logout-handle request)
        nil))

(defn wrapping-handler [request]
  (try
   (if-let [resp (route-handler request)]
    resp
    {:status 404 :body (str "Not found" (:uri request))})

  (catch Throwable e
    {:status 500 :body (apply str (interpose "\n" (.getStackTrace e)))})))


;;(def full-handler
;;  (simple-log-middleware wrapping-handler))

;;(def full-handler
;;  (not-found-middleware (simple-log-middleware route-handler)))

(def full-handler
  (-> route-handler
   not-found-middleware
   (resource/wrap-resource "public")
   file-info/wrap-file-info
   wrap-case-middleware
   wrap-exception-middleware
   ring.middleware.keyword-params/wrap-keyword-params
   ring.middleware.params/wrap-params
   ring.middleware.multipart-params/wrap-multipart-params
   ring.middleware.cookies/wrap-cookies
   (ring.middleware.session/wrap-session
    {:cookie-name "ring-session"
     :root "/"
     :cookie-attrs {:max-age 600
                    :secure false}
     :store (ring.middleware.session.memory/memory-store)})
   simple-log-middleware))

