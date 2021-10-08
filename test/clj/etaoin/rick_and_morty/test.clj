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

(def ^:dynamic *driver*)

(defn driver-fixture
  [f]
  (api/with-firefox {} driver
    (binding [*driver* driver]
      (api/go *driver* "http://localhost")
      (f))))

(use-fixtures :each driver-fixture)

(deftest should-load-characters
  (testing "...."
    (api/wait-visible *driver* :load-btn)
    (load-characters *driver*)
    (api/get-element-inner-html *driver* {:class "card mb-4 ch-card"})
    (is true)))


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