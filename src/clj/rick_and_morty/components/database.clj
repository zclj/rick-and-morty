(ns rick-and-morty.components.database
  (:require [crux.api :as crux]
            [integrant.core :as ig]
            [rick-and-morty.stuff :as stuff]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; component lifecycle
;; 

(defmethod ig/init-key :app/database
  [_ _]
  (println "Initializing" :app/database)
  (let [node (crux/start-node {})]
    {:node        node
     :stuff/query (partial stuff/query node)}))

(defmethod ig/halt-key! :app/database
  [_ {:keys [node]}]
  (println "Shutting down" :app/database)
  (.close node))
