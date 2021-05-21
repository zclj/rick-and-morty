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
  ([db]
   (let [res (query-characters db [:name])]
     (into #{} (map
                (fn [[ch]] [(:name ch)])
                res))))
  ([db query]
   (crux/q (crux/db db)
           {:find  [(list 'pull 'e query)]
            :where [['e :name]]})))

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

  (def n (crux/start-node {}))
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
