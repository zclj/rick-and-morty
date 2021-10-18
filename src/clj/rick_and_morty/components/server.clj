(ns rick-and-morty.components.server
  (:require [clojure.core.async :as async]
            [clojure.pprint :refer [pprint]]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [integrant.core :as ig]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :refer [body-params]]
            [io.pedestal.interceptor :as interceptor]
            [malli.core :as m]
            [ring.util.response :as ring-resp]
            [rick-and-morty.components.database :as db]
            [rick-and-morty.ram :as ram]))

;; clojure.spec

(s/def :load-characters/attrs #{"id" "name" "gender" "image"})

(s/def :load-characters/json-params
       (s/coll-of :load-characters/attrs :distinct true))

(s/def :load-characters/content-type #{"application/json"})

(s/def :load-characters/request
       (s/keys :req-un [:load-characters/json-params
                        :load-characters/content-type]))

(defn spec-validator
  [spec x]
  (if (s/valid? spec x)
    {:success? true}
    {:success? false
     :message  (s/explain-str spec x)}))

(comment
  (require '[clojure.spec.gen.alpha :as gen])

  (gen/sample (s/gen :load-characters/request) 5)
  
  ;;
  )

;; malli

(def load-characters-spec
  [:map 
   [:content-type [:enum "application/json"]]
   [:json-params [:sequential [:enum "id" "name" "gender" "image"]]]])

(defn malli-validator
  [spec x]
  (if (m/validate spec x)
    {:success? true}
    {:success? false
     :message (m/explain spec x)}))

(comment
  (require '[malli.generator :as mg])

  (mg/generate load-characters-spec)
  (m/validate load-characters-spec {:content-type "application/json"
                                    :json-params ["id" "name" "id"]})
  ;;
  )

;; impl

(defn validate-interceptor
  [validator]
  {:name  ::validate-interceptor
   :enter (fn [{:keys [request] :as context}]
            (log/info :interceptor :validate)
            (let [{:keys [success? message]} (validator request)]
              (if success?
                context
                (do
                  (log/info :interceptor :validate :failure message)
                  (assoc context :response {:status  400
                                            :headers {}
                                            :body    message})))))})

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
    ["/api/v1/populate"
     :post
     [populate-interceptor]
     :route-name ::populate]
    ["/api/v1/characters"
     :post
     (conj json-interceptors 
           (validate-interceptor (partial spec-validator :load-characters/request))
          ;;  (validate-interceptor (partial malli-validator load-characters-spec))
           characters-interceptor)
     :route-name ::characters]})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component lifecycle
;; 

(defmethod ig/init-key :app/server
  [_ {:keys [config] :as deps}]
  (println "Initializing" :app/server)
  (let [server (->
                {::http/type           :jetty
                 ::http/host           "0.0.0.0"
                 ::http/port           80
                 ::http/join?          (:join? config)
                 ::http/routes         (routes)
                 ::http/resource-path  "/public"
                 ::http/secure-headers {:content-security-policy-settings {}}}
                (http/default-interceptors)
                (update ::http/interceptors conj (deps-interceptor deps))
                (http/create-server))]
    {:pedestal (http/start server)}))

(defmethod ig/halt-key! :app/server
  [_ {:keys [pedestal]}]
  (println "Shutting down" :app/server)
  (http/stop pedestal))

