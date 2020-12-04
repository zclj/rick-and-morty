(ns rick-and-morty.main
  (:require [clojure.pprint :refer [pprint]]
            [crux.api :as crux]
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

(def get-stuff
  {:name :get-stuff
   :enter (fn [context]
            (let [stuff    (query-stuff @crux-node)
                  response {:status  200
                            :body    stuff}]
              (assoc context :response response)))})

(def routes #{["/"     :get #'hello :route-name ::root]
              ["/echo" :any #'echo  :route-name ::echo]
              ["/api/v1/stuff" :get (conj json-interceptors #'get-stuff) :route-name ::get-stuff]})

(defonce pedestal (atom nil))

(defn start-pedestal
  ([] (start-pedestal false))
  ([join?]
   (when (nil? @pedestal)
     (let [server-map {::http/type          :jetty
                       ::http/host          "0.0.0.0"
                       ::http/port          80
                       ::http/join?         join?
                       ::http/routes        routes
                       ::http/resource-path "/public"}
           server (http/create-server server-map)]
       (reset! pedestal (http/start server))))))

(defn stop-pedestal
  []
  (when (some? @pedestal)
    (http/stop (deref pedestal))
    (reset! pedestal nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce crux-node (atom nil))

(defn start-crux
  []
  (when (nil? @crux-node)
    (reset! crux-node (crux/start-node {}))))

(defn stop-crux
  []
  (when (some? @crux-node)
    (.close @crux-node)
    (reset! crux-node nil)))

(defn query-stuff
  [node]
  (crux/q (crux/db node)
          '{:find  [name desc]
            :where [[e :name name]
                    [e :desc desc]]}))

(comment
  ;;

  (crux/submit-tx @crux-node [[:crux.tx/put {:crux.db/id :foo
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

  ;;
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start
  [& {:keys [join?]}]
  (start-crux)
  (start-pedestal (true? join?)))

(defn stop
  []
  (stop-pedestal)
  (stop-crux))

(defn reset
  []
  (stop)
  (start))

(defn -main
  [& args]
  (start :join? true))
