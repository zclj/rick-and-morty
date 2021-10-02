(ns rick-and-morty.components.database
  (:require [clojure.java.io :as io]
            [xtdb.api :as xt]
            [integrant.core :as ig]))

(defonce node-db (atom nil))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; internal fns
;; 

(defn make-ch-tx
  [{:keys [id] :as ch}]
  [::xt/put (assoc ch :xt/id id)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public db API
;; 

(defn store-characters
  [db characters]
  (let [ch-txs (mapv make-ch-tx characters)]
    (xt/submit-tx db ch-txs)))

(defn query-characters
  ([db]
   (let [res (query-characters db [:name])]
     (into #{} (map
                (fn [[ch]] [(:name ch)])
                res))))
  ([db query]
   (xt/q (xt/db db)
         {:find  [(list 'pull 'e query)]
          :where [['e :name]]})))

(comment
  ;;

  (query-characters @node-db)

  (def sample
    [{:name "Antenna Rick"
      :species "Human"
      :type "Human with antennae"
      :created "2017-11-04T22:28:13.756Z"
      :status "unknown"
      :id 19
      :url "https://rickandmortyapi.com/api/character/19"
      :image "https://rickandmortyapi.com/api/character/avatar/19.jpeg"
      :origin {:name "unknown", :url ""}
      :gender "Male"
      :episode ["https://rickandmortyapi.com/api/episode/10"]
      :location {:name "unknown", :url ""}}
     {:name "Ants in my Eyes Johnson"
      :species "Human"
      :type "Human with ants in his eyes"
      :created "2017-11-04T22:34:53.659Z"
      :status "unknown"
      :id 20
      :url "https://rickandmortyapi.com/api/character/20"
      :image "https://rickandmortyapi.com/api/character/avatar/20.jpeg"
      :origin {:name "unknown", :url ""}
      :gender "Male"
      :episode ["https://rickandmortyapi.com/api/episode/8"]
      :location
      {:name "Interdimensional Cable"
       :url "https://rickandmortyapi.com/api/location/6"}}])

  (def n (xt/start-node {}))
  (store-characters n sample)

  (mapv make-ch-tx sample)
  (make-ch-tx (first sample))


  (let [res #{[{:name "Antenna Rick", :status "unknown", :gender "Male"}]
              [{:name "Ants in my Eyes Johnson", :status "unknown", :gender "Male"}]}]
    (into #{} (map
               (fn [[ch]] [(:name ch)])
               res)))

  [(:name (first [{:name "Antenna Rick", :status "unknown", :gender "Male"}]))]

  (query-characters n)
  (query-characters n [:name :status :gender])

  ;;
  )

(defn start-xtdb! []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir (io/file dir)
                        :sync? true}})]
    (xt/start-node
     {:xtdb/tx-log         (kv-store "target/data/dev/tx-log")
      :xtdb/document-store (kv-store "target/data/dev/doc-store")
      :xtdb/index-store    (kv-store "target/data/dev/index-store")
      })))

(comment
  (start-xtdb!)
  ;;
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component lifecycle
;; 

(defmethod ig/init-key :app/database
  [_ _]
  (println "Initializing" :app/database)
  (let [node (start-xtdb!)]
    (reset! node-db node)
    {:node node}))

(defmethod ig/halt-key! :app/database
  [_ {:keys [node]}]
  (println "Shutting down" :app/database)
  (.close node)
  (reset! node-db nil))
