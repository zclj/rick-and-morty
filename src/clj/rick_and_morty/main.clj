(ns rick-and-morty.main
  (:require [crux.api :as crux]
            [rick-and-morty.app :as app]
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
  (restart)

  (crux/submit-tx (get-in @app/components [:app/database :node])
                  [[:crux.tx/put {:crux.db/id :foo
                                  :name "foo"
                                  :desc "foO description text in crux"}]
                   [:crux.tx/put {:crux.db/id :bar
                                  :name "bar"
                                  :desc "Bar description text in crux"}]
                   [:crux.tx/put {:crux.db/id :baz
                                  :name "baz"
                                  :desc "BAZ description text in crux"}]
                   [:crux.tx/put {:crux.db/id :gizmo
                                  :name "gizmo"
                                  :desc "GiZmO description text in crux"}]])

  (crux/submit-tx (get-in @app/components [:app/database :node])
                  [[:crux.tx/put {:crux.db/id :stefan
                                  :name "Stefan"
                                  :desc "Stefan stefan stefan"}]])
  

  (seq {:foo {:baz 3} :bar 2})

  ;;
  )

