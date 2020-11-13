(ns rick-and-morty.main
  (:require [io.pedestal.http :as http]))

(defn hello
  [request]
  {:status 200
   :body "Rick and Morty app!"})

(defn echo
  [request]
  {:status 200
   :body "Should echo request here"})

(def routes #{["/"     :get hello :route-name ::root]
              ["/echo" :any echo  :route-name ::echo]})

(defn start
  []
  (let [server-map {::http/type   :jetty
                    ::http/host   "0.0.0.0"
                    ::http/port   80
                    ::http/join?  true
                    ::http/routes routes}
        server (http/create-server server-map)]
    (http/start server)))

(defn -main
  [& args]
  (start))
