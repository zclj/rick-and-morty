(ns rick-and-morty.stuff
  (:require [crux.api :as crux]))

(defn query
  [node]
  (crux/q (crux/db node)
          '{:find  [name desc]
            :where [[e :name name]
                    [e :desc desc]]}))
