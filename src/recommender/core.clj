(ns recommender.core
  (:require [clojure.tools.cli :refer [cli]]
            [recommender.engine :refer [add-rating
                                        calculate-item-probability
                                        update-suggested-items
                                        get-user-suggestions
                                        update-similar-users
                                        flush-db]])
  (:gen-class))

(defn rate
  "enter item rating for user"
  [user, item, rating]
  (println "User: " user "ranked item: " item "with: " rating)
  (add-rating user item rating))

(defn get-probability
  "get probability user will like item"
  [user item]
  (let [score (calculate-item-probability user item)]
    (println user ":" item ":" score)))

(defn suggest
  "suggest items for user"
  [user max]
  (println "Retrieving " max "results for user: " user)
  (update-suggested-items user (dec max))
  (let [suggestions (get-user-suggestions user (dec max))]
    (println "Results: " suggestions)))

(defn update-db
  "update database - required after additional ratings"
  [max]
  (println "Updating DB")
  (update-similar-users (dec max)))

(def clear-db
  "clears database completely"
  (flush-db))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "currently only available in the REPL"))
