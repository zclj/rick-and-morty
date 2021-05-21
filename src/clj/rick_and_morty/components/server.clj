(ns rick-and-morty.components.server
  (:require [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :refer [body-params]]
            [io.pedestal.interceptor :as interceptor]
            [ring.util.response :as ring-resp]
            [rick-and-morty.components.database :as db]
            [rick-and-morty.ram :as ram]))

(def json-interceptors [(body-params) http/json-body])

(defn redirect-interceptor
  [to]
  {:name  ::redirect-interceptor
   :enter (fn [context]
            (assoc context :response (ring-resp/redirect to)))})

(defn deps-interceptor
  [deps]
  (interceptor/interceptor
   {:name  ::deps-interceptor
    :enter (fn [context] (merge context deps))}))

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

(def populate-interceptor
  {:name  ::populate-interceptor
   :enter (fn [context]
            (let [response {:status 201}
                  node     (get-in context [:database :node])]
              (load-characters node)
              (assoc context :response response)))})

(def characters-interceptor
  {:name  ::characters-interceptor
   :enter (fn [context]
            (pprint (get-in context [:request :json-params]))
            (let [query      (into [] (map keyword (get-in context [:request :json-params])))
                  node       (get-in context [:database :node])
                  characters (db/query-characters node query)]
              (assoc context :response {:status 200
                                        :body   characters})))})

(defn routes
  []
  #{["/"     :get (redirect-interceptor "/index.html") :route-name ::root]
    ["/echo" :any [echo]  :route-name ::echo]
    ["/api/v1/populate" :post [populate-interceptor] :route-name ::populate]
    ["/api/v1/characters" :post (conj json-interceptors characters-interceptor)]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component lifecycle
;; 

(defmethod ig/init-key :app/server
  [_ {:keys [config] :as deps}]
  (println "Initializing" :app/server)
  (let [server (->
                {::http/type          :jetty
                 ::http/host          "0.0.0.0"
                 ::http/port          80
                 ::http/join?         (:join? config)
                 ::http/routes        (routes)
                 ::http/resource-path "/public"}
                (http/default-interceptors)
                (update ::http/interceptors conj (deps-interceptor deps))
                (http/create-server))]
    {:pedestal (http/start server)}))

(defmethod ig/halt-key! :app/server
  [_ {:keys [pedestal]}]
  (println "Shutting down" :app/server)
  (http/stop pedestal))

