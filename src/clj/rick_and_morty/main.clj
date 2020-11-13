(ns rick-and-morty.main
  (:require [clojure.pprint :refer [pprint]]
            [io.pedestal.http :as http]))

(defn hello
  [request]
  {:status 200
   :body "Rick and Morty app!!!"})

(def echo
  {:name :echo
   :enter (fn [context]
            (let [response {:status 200
                            :headers {"Content-Type" "text/plain"}
                            :body (with-out-str (pprint context))}]
              (assoc context :response response)))})

(def routes #{["/"     :get #'hello :route-name ::root]
              ["/echo" :any #'echo  :route-name ::echo]})

(defonce instance (atom nil))

(defn start
  ([] (start false))
  ([join?]
   (when (nil? @instance)
     (let [server-map {::http/type          :jetty
                       ::http/host          "0.0.0.0"
                       ::http/port          80
                       ::http/join?         join?
                       ::http/routes        routes
                       ::http/resource-path "/public"}
           server (http/create-server server-map)]
       (reset! instance (http/start server))))))

(defn stop
  []
  (when (some? @instance)
    (http/stop (deref instance))
    (reset! instance nil)))

(defn reset
  []
  (stop)
  (start))

(defn -main
  [& args]
  (start true))
