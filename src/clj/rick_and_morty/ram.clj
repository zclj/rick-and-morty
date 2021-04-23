(ns rick-and-morty.ram
  (:require [clj-http.client :as http]
            [cheshire.core :as json]))

(def base-url "https://rickandmortyapi.com/api")

(defn get-characters
  []
  (let [res (http/get (str base-url "/character"))
        body (:body res)]
    (-> body
        (json/parse-string true)
        (:results))))

(comment
  ;;

  (get-characters)

  ;;
  )