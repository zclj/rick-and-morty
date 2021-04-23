(ns rick-and-morty.main
  (:require [cljs.core.async :as async]
            [clojure.string :as string]
            [clojure.pprint :refer [pprint]]
            [cljs-http.client :as http]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(defonce state (r/atom {}))

(defn load-stuff!
  []
  (async/go (let [res-ch (http/get "/api/v1/stuff")
                  res    (async/<! res-ch)]
              (swap! state assoc :stuff (:body res)))))

(defn populate
  []
  (async/go 
   (let [res (async/<! (http/post "/api/v1/populate"))]
     (js/console.log (with-out-str (pprint res))))))

(defn state-presenter
  []
  (let [stuff (:stuff @state)]
    [:div
     [:ul (map 
           (fn [s]
             ^{:key (first s)}
             [:div 
              [:h1 (first s)] 
              [:h2 (second s)]])
           stuff)]]))

(defn root
  []
  [:div
   [:div.hero.is-info
    [:div.hero-body
     [:div.container
      [:h1.title "Rick and Morty"]
      [:h2.subtitle "This is an SPA developed in Clojurescript and Reagent (React)"]]]]
   [:div.content.m-6
    [:button.button
     {:on-click populate}
     "Populate DB"]
    [:button.button.is-link
     {:on-click load-stuff!}
     "Get stuff"]
    [state-presenter]]])

(defn start
  []
  (rdom/render [root] (js/document.getElementById "app")))

(defn ^:dev/after-load restart
  []
  (start))

(defn init!
  []
  (start))
