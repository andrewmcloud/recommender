(ns recommender.core
  (:require [clojure.tools.cli :refer [cli]]
            [recommender.engine :refer [add-rating
                                        calculate-item-probability
                                        update-suggested-items
                                        get-user-suggestions
                                        update-similar-users]])
  (:gen-class))

(defn rate
  [user, item, rating]
  (println "User: " user "ranked item: " item "with: " rating)
  (add-rating user item rating))

(defn getProbability
  [user item]
  (let [score (calculate-item-probability user item)]
    (println user ":" item ":" score)))

(defn suggest
  [user max]
  (println "Retrieving " max "results for user: " user)
  (update-suggested-items user max)
  (let [suggestions (get-user-suggestions user max)]
    (println "Results: " suggestions)))

(defn update
  [max]
  (println "Updateing DB")
  (update-similar-users max))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
