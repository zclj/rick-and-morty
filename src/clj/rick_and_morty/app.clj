(ns rick-and-morty.app
  (:require [integrant.core :as ig]))

(defn component-map
  [join?]
  {:app/server   {:database (ig/ref :app/database)
                  :config   {:join? join?}}
   :app/database {}})

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
