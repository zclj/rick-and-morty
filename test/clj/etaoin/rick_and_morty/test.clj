(ns rick-and-morty.test
  (:require [clojure.test :refer [use-fixtures deftest testing is]]
            [etaoin.api :as api]))

(defn load-characters
  [driver]
  (when-let [btn (api/query driver :load-btn)]
    (api/click-el driver btn)))

(defn clear-characters
  [driver]
  (when-let [btn (api/query driver :clear-btn)]
    (api/click-el driver btn)))

(defn wait-load-button-visible
  [driver]
  (api/wait-visible driver :load-btn))

(defn wait-characters-visible
  [driver]
  (api/wait-visible driver {:class "card mb-4 ch-card"}))

(defn wait-characters-absent
  [driver]
  (api/wait-absent driver {:class "card mb-4 ch-card"}))

(def ^:dynamic *driver*)

(defn driver-fixture
  [f]
  (api/with-firefox-headless {} driver
    (binding [*driver* driver]
      (api/go *driver* "http://localhost")
      (f))))

(use-fixtures :each driver-fixture)

(deftest should-load-characters
  (testing "...."
    (wait-load-button-visible *driver*)
    (load-characters *driver*)
    (wait-characters-visible *driver*)
    (api/screenshot *driver* "target/loaded-characters.png")
    (is "Character list loaded")))

(deftest should-clear-characters
  (testing "...."
    (wait-load-button-visible *driver*)
    (load-characters *driver*)
    (wait-characters-visible *driver*)
    (clear-characters *driver*)
    (wait-characters-absent *driver*)
    (api/screenshot *driver* "target/cleared-characters.png")
    (is "Character list cleared")))


(comment
  (def driver (api/firefox))

  (api/go driver "http://localhost")

  (api/get-element-inner-html driver [{:id :load-btn}])
  (api/get-element-inner-html driver :clear-btn)

  (api/query-all driver {:fn/has-classes [:ch-card]})
  (api/query-all driver {:fn/has-classes [:card-header-icon]})

  (doall (map #(api/click-el driver %)
              (api/query-all driver {:fn/has-classes [:card-header-icon]})))

  (let [btn (api/query driver {:id :load-btn})]
    (api/click-el driver btn))

  (api/click driver :load-btn)
  (api/click driver :clear-btn)

  (load-characters driver)
  (clear-characters driver)
  
  ;;
  )