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
  (async/go (let [res-ch (http/post "/api/v1/characters" {:json-params [:id :name :gender :image]})
                  res    (async/<! res-ch)
                  chs    (reduce (fn [acc [{:keys [id] :as ch}]]
                                   (assoc acc id ch))
                                 {}
                                 (:body res))]
              (js/console.log (with-out-str (pprint chs)))
              (swap! state assoc :characters chs))))

(defn populate
  []
  (async/go
    (let [res (async/<! (http/post "/api/v1/populate"))]
      (js/console.log (with-out-str (pprint res))))))

(defn ch-card
  [ch]
  [:div.card.mb-4.ch-card
   [:header.card-header
    [:p.card-header-title (:name ch)]
    [:a.card-header-icon
     {:on-click (fn [] 
                  (swap! state assoc-in [:characters (:id ch)] (assoc ch :expanded (not (:expanded ch)))))}
     [:span.icon
      (if (:expanded ch)
        [:i.fa.fa-angle-up]
        [:i.fa.fa-angle-down])]]]
   (when (:expanded ch)
     [:div
      [:div.card-image
       [:img {:src (:image ch)}]]
      [:div.card-content
       [:h2 (:name ch)]]])
   ])

(defn state-presenter
  []
  (let [characters (:characters @state)]
    [:div
     [:ul#character-list (map
                          (fn [[id ch]]
                            ^{:key id}
                            [ch-card ch])
                          characters)]]))

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
    [:button#load-btn.button.is-link
     {:on-click load-stuff!}
     "Get characters"]
    [:button.button
     {:on-click (fn [] (js/console.log (with-out-str (pprint @state))))}
     "Dump state"]
    [:button#clear-btn.button
     {:on-click (fn [] (swap! state dissoc :characters))}
     "Clear"]
    [state-presenter]
    ]])

(defn start
  []
  (rdom/render [root] (js/document.getElementById "app")))

(defn ^:dev/after-load restart
  []
  (start))

(defn init!
  []
  (start))
