(ns rick-and-morty.main
  (:require [clojure.pprint :refer [pprint]]
            [crux.api :as crux]
            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :refer [body-params]]))

(declare query-stuff)
(declare crux-node)

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

(defn get-stuff
  [node]
  {:name :get-stuff
   :enter (fn [context]
            (let [stuff    (query-stuff node)
                  response {:status  200
                            :body    stuff}]
              (assoc context :response response)))})

(defn routes
  [node]
  #{["/"     :get #'hello :route-name ::root]
    ["/echo" :any #'echo  :route-name ::echo]
    ["/api/v1/stuff" :get (conj json-interceptors (get-stuff node)) :route-name ::get-stuff]})

(defmethod ig/init-key ::server
  [_ {:keys [config database]}]
  (println "Initializing" ::server)
  (let [server-map {::http/type          :jetty
                    ::http/host          "0.0.0.0"
                    ::http/port          80
                    ::http/join?         (:join? config)
                    ::http/routes        (routes (:node database))
                    ::http/resource-path "/public"}
        server (http/create-server server-map)]
    {:pedestal (http/start server)}))

(defmethod ig/halt-key! ::server
  [_ {:keys [pedestal]}]
  (println "Shutting down" ::server)
  (http/stop pedestal))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn query-stuff
  [node]
  (crux/q (crux/db node)
          '{:find  [name desc]
            :where [[e :name name]
                    [e :desc desc]]}))

(defmethod ig/init-key ::database
  [_ _]
  (println "Initializing" ::database)
  {:node (crux/start-node {})})

(defmethod ig/halt-key! ::database
  [_ {:keys [node]}]
  (println "Shutting down" ::database)
  (.close node))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn component-map
  [join?]
  {::server   {:database (ig/ref ::database)
               :config   {:join? join?}}
   ::database {}})

(defonce components (atom nil))

(defn init-components
  [join?]
  (when (nil? @components)
    (reset! components (ig/init (component-map join?)))))

(defn halt-components
  []
  (when (some? @components)
    (ig/halt! @components)
    (reset! components nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start
  [& {:keys [join?]}]
  (init-components join?))

(defn stop
  []
  (halt-components))

(defn reset
  []
  (stop)
  (start))

(defn -main
  [& args]
  (start :join? true))

(comment
  ;;

  (crux/submit-tx (get-in @components [::database :node])
                  [[:crux.tx/put {:crux.db/id :foo
                                  :name "foo"
                                  :desc "foO description text in crux"}]
                   [:crux.tx/put {:crux.db/id :bar
                                  :name "bar"
                                  :desc "Bar description text in crux"}]
                   [:crux.tx/put {:crux.db/id :baz
                                  :name "baz"
                                  :desc "BAZ description text in crux"}]
                   [:crux.tx/put {:crux.db/id :gizmo
                                  :name "gizmo"
                                  :desc "GiZmO description text in crux"}]])

  (crux/submit-tx (get-in @components [::database :node])
                  [[:crux.tx/put {:crux.db/id :stefan
                                  :name "Stefan"
                                  :desc "Stefan stefan stefan"}]])

  ;;
  )

