(ns rick-and-morty.components.database
  (:require [crux.api :as crux]
            [integrant.core :as ig]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; internal fns
;; 

(defn make-ch-tx
  [{:keys [id] :as ch}]
  [:crux.tx/put (assoc ch :crux.db/id id)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; public db API
;; 

(defn store-characters
  [db characters]
  (let [ch-txs (mapv make-ch-tx characters)]
    (crux/submit-tx db ch-txs)))

(defn query-characters
  [db]
  (crux/q (crux/db db)
          '{:find  [name]
            :where [[e :name name]]}))

(comment
  ;;

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
      :location {:name "unknown", :url ""}}])
  
  (mapv make-ch-tx sample)
  (make-ch-tx (first sample))

  ;;
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component lifecycle
;; 

(defmethod ig/init-key :app/database
  [_ _]
  (println "Initializing" :app/database)
  (let [node (crux/start-node {})]
    {:node node}))

(defmethod ig/halt-key! :app/database
  [_ {:keys [node]}]
  (println "Shutting down" :app/database)
  (.close node))
