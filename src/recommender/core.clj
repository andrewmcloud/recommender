(ns recommender.core
  (:require [clojure.string :refer [join]])
  (:gen-class))


(defn add-rating
  "add user rating for item
   redis ZADD user:user-id:items, rating, item-id"
  [user-id, item-id, rating])

(defn get-user-items
  "return list of items rated by user
   redis ZREVRANGE user:user-id:items, 0, max-items"
  [user-id, max-items])

(defn get-user-suggestions
  "return list of user suggestions
   redis ZREVRANGE user:user-id:suggestions, 0 max-suggestions"
  [user-id, max-suggestions])

(defn get-item-ratings
  "return list rating an item has received
   redis ZREVRANGE item:item-id:scores, 0, max-ratings"
  [item-id, max-ratings])

(defn mean
  "cacluate mean"
  [count sum]
  (/ sum count))

(defn rms
  "calculate the Root Mean Square for collection
  (sqrt (/ (reduce + (map #(* % %) user-difs)) (count user-diffs)))"
  [coll]
  (->>
    coll
    (map #(* % %))
    (reduce +)
    (mean (count coll))
    (Math/sqrt)))

(defn calculate-similarity
  "calculates the RMS of the intersection between items rated by two users"
  [user1-id user2-id])
  ;
  ;(let [_ ZINTERSTORE, ztmp, 2, user:user1-id:items user:user2-id:items, WEIGHTS, 1, -1
  ;      user-diffs (redis ZRANGE ztmp, 0 -1, WITHSCORES)]
  ;  (rms user-diffs)

(defn calculate-item-probability
  "calculates average rating similar users gave an item"
  [user item])
  ;(redis ZINTERSTORE user:user-id:similars item:item-id:ratings WEIGHTS 0 1))
  ;(let [scores (redis ZRANGE ztmp 0 -1 WITHSCORES
  ;      ct (count scores)]
  ;  (if (zero? scores)
  ;    0
  ;    (->> scores
  ;       (reduce +)
  ;       (mean ct)))))

(defn update-similar-users
  "updates the list of similar users for each user"
  [max-sim-users])
  ;users = SMEMBERS, user
  ;(for [user-id users
  ;      :let [candidates (get-similar-candidates user max)]
  ;  (for [candidate-id candidates
  ;        :when (!= candidate-id user-id)
  ;    (let [score (calculate-similarity user-id candidate-id)]
  ;      (redis ZADD user:user-id:similarusers, similarity-score, candidate-id)])

(defn get-suggested-candidates
  "returns list of similar users"
  [user-id max-candidates]
  (let [sim-users [1 2 3 4 5] ;(redis ZRANGE user:user-id:similars, 0, max-candidates)]
        ct (count sim-users)
        user-item-sets (reduce (fn [coll sim-user-id]
                                 (conj coll (str "user:" sim-user-id ":items")))
                               []
                               sim-users)]))
    ;(redis ZUNIONSTORE ztmp, ct, (join " " user-item-sets), AGREGATE, MIN))
    ;(redis ZRANGEBYSCORE, ztmp, 0, inf

(defn get-similarity-candidates
  ""
  [user-id max])
  ;(let [items (get-user-items user-id max)
  ;      ct (count items)
  ;      item-ranking-sets (reduce (fn [coll item-id]
  ;                                  (conj coll (str "item:" item-id ":scores")))
  ;                                []
  ;                                items)]
  ;  (redis ZUNIONSTORE ztmp, ct, (join " " item-ranking-sets)
  ;  (redis ZRANGE ztmp 0 -1)



(defn update-suggested-items
  "updates suggested items for user"
  [user max-items]
  (let [items (get-suggested-candidates user-id max-items)
        max-items (count items)]))

    ;(for [item items]
    ;  (redis ZADD user:user-id:suggestions (calculate-item-probability user item) item)))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
