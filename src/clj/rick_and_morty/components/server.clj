(ns rick-and-morty.components.server
  (:require [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :refer [body-params]]
            [rick-and-morty.components.database :as db]
            [rick-and-morty.ram :as ram]))

(def json-interceptors [(body-params) http/json-body])

(defn hello
  [request]
  {:status 200
   :body "Rick and Morty app!!!"})

(def echo
  {:name :echo
   :enter (fn [context]
            (let [response {:status  200
                            :headers {"Content-Type" "text/plain"}
                            :body    (with-out-str (pprint context))}]
              (assoc context :response response)))})

(defn load-characters
  [db]
  (async/thread
    (let [characters (ram/get-characters)]
      (db/store-characters db characters)
      (pprint characters))))

(defn populate-interceptor
  [db]
  {:name  ::populate-interceptor
   :enter (fn [context]
            (let [response {:status 201}]
              (load-characters db)
              (assoc context :response response)))})

(defn characters-interceptor
  [db]
  {:name  ::characters-interceptor
   :enter (fn [context]
            (let [characters (db/query-characters db)]
              (assoc context :response {:status 200
                                        :body   characters})))})

(defn routes
  [db]
  #{["/"     :get #'hello :route-name ::root]
    ["/echo" :any #'echo  :route-name ::echo]
    ["/api/v1/populate" :post [(populate-interceptor db)] :route-name ::populate]
    ["/api/v1/characters" :get (conj json-interceptors (characters-interceptor db))]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component lifecycle
;; 

(defmethod ig/init-key :app/server
  [_ {:keys [config database]}]
  (println "Initializing" :app/server)
  (let [server-map {::http/type          :jetty
                    ::http/host          "0.0.0.0"
                    ::http/port          80
                    ::http/join?         (:join? config)
                    ::http/routes        (routes (:node database))
                    ::http/resource-path "/public"}
        server (http/create-server server-map)]
    {:pedestal (http/start server)}))

(defmethod ig/halt-key! :app/server
  [_ {:keys [pedestal]}]
  (println "Shutting down" :app/server)
  (http/stop pedestal))

