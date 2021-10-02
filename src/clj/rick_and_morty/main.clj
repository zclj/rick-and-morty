(ns rick-and-morty.main
  (:require [rick-and-morty.app :as app]
            [rick-and-morty.components.database]
            [rick-and-morty.components.server]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start
  [& {:keys [join?]}]
  (app/init-components join?))

(defn stop
  []
  (app/halt-components))

(defn restart
  []
  (stop)
  (start))

(defn -main
  [& args]
  (start :join? true))

(comment
  ;;

  (start)
  (stop)
  (restart)

  ;;
  )

