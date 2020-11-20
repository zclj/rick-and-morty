(ns rick-and-morty.main
  (:require [clojure.string :as string]
            [reagent.dom :as rdom]))

(defn root
  []
  [:div
   [:h1 (string/join " " ["Rick" "and" "Morty!"])]
   [:p "This is an SPA developed in Clojurescript and Reagent (React)"]])

(defn start
  []
  (rdom/render [root] (js/document.getElementById "app")))

(defn ^:dev/after-load restart
  []
  (println (meta #'restart))
  (start))

(defn init!
  []
  (start))
