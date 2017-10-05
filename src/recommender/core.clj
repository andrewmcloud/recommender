(ns recommender.core
  (:require [clojure.string :refer [join]]
            [taoensso.carmine :as car :refer [wcar]])
  (:gen-class))

(def redis-conn {:pool {} :spec {:uri "http://127.0.0.1:6379"}})
(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

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

(defn remove-key
  [key]
  (wcar*
    (car/del key)))

(defn add-rating
  "add user rating for item
   redis ZADD user:user-id:items, rating, item-id"
  [user, item, rating]
  (wcar*
    (car/zadd (str "user:" user ":items") rating item)))

(defn get-user-items
  "return list of items rated by user
   redis ZREVRANGE user:user-id:items, 0, max-items"
  [user, max-items]
  (wcar*
    (car/zrevrange (str "user:" user ":items") 0 max-items)))

(defn get-user-suggestions
  "return list of user suggestions
   redis ZREVRANGE user:user-id:suggestions, 0 max-suggestions"
  [user, max-suggestions]
  (wcar*
    (car/zrevrange (str "user-id:" user ":suggestions") 0 max-suggestions)))

(defn get-item-ratings
  "return list rating an item has received
   redis ZREVRANGE item:item-id:scores, 0, max-ratings"
  [item, max-ratings]
  (wcar*
    (car/zrevrange (str "item:" item ":ratings") 0 max-ratings)))

(defn calculate-similarity
  "calculates the RMS of the intersection between items rated by two users"
  [user1-id user2-id]
  (let [_ (wcar*
            (car/zinterstore* "ztmp"
                              (str "user:" user1-id ":items")
                              (str "user:" user2-id ":items")
                              "WEIGHTS" 1, -1))
        user-diffs (wcar*
                     (car/zrange "ztmp" 0 -1 "WITHSCORES"))]
    (remove-key "ztmp")
    (rms user-diffs)))

(defn calculate-item-probability
  "calculates average rating similar users gave an item"
  [user item]
  (wcar*
    (car/zinterstore* "ztmp"
                      (str "user:" user ":similars")
                      (str "item:" item ":ratings")))

  (let [scores (wcar*
                 (car/zrange "ztmp" 0 -1 "WITHSCORES"))
        ct (count scores)]
    (if (zero? ct)
      0
      (->> scores
         (reduce +)
         (mean ct)))))

(defn get-similar-candidates
  "return vector of similar users based on item ratings"
  [user max]
  (let [items (get-user-items user max)
        ct (count items)
        item-ranking-sets (reduce (fn [coll item]
                                    (conj coll (str "item:" item ":scores")))
                                  []
                                  items)]
    (nth (wcar*
            (car/zunionstore* "ztmp" (join " " item-ranking-sets))
            (car/zrange "ztmp" 0 -1)
            (car/del "ztmp"))
         1))) ;only care about zrange results

(defn update-similar-users
  "updates the sorted set of similar users for each user"
  [max-sim-users]
  (let [users (wcar* (car/smembers "users"))]
    (if (empty? users)
      nil
      (do
        (for [user users
              :let [candidates (get-similar-candidates user max-sim-users)]]
          (for [candidate candidates
                :when (not (= candidate user))]
            (let [similarity-score (calculate-similarity user candidate)]
              (wcar* (car/zadd "user:" user ":similarusers") similarity-score, candidate))))))))

(defn get-suggested-candidates
  "returns vector of similar users"
  [user max-candidates]
  (let [sim-users (wcar*
                    (car/zrange (str "user:" user ":similars") 0 max-candidates))
        ct (count sim-users)
        user-item-sets (reduce (fn [coll sim-user]
                                 (conj coll (str "user:" sim-user ":items")))
                               []
                               sim-users)]
    (nth (wcar*
           (car/zunionstore* "ztmp" (join " " user-item-sets) "AGREGATE" "MIN")
           (car/zrangebyscore "ztmp" 0 "INF")
           (car/del "ztmp"))
         1))) ;only care about zrangebyscore

(defn update-suggested-items
  "updates suggested items for user"
  [user max-items]
  (let [items (get-suggested-candidates user max-items)
        max-items (count items)]
    (map (fn [item]
           (wcar*
             (car/zadd (str "user:" user ":suggestions") (calculate-similarity user item) item))))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
