(ns recommender.engine
  (:require [clojure.string :refer [join]]
            [taoensso.carmine :as car :refer [wcar]]))

(def redis-conn {:pool {} :spec {:uri "http://127.0.0.1:6379"}})
(defmacro wcar* [& body] `(car/wcar redis-conn ~@body))

(defn mean
  "cacluate mean"
  [count sum]
  (/ sum count))

(defn rms
  "calculate the Root Mean Square for collection"
  [coll]
  (->>
    coll
    (map #(* % %))
    (reduce +)
    (mean (count coll))
    (Math/sqrt)))

(defn parse-redis-response-int
  [rsp]
  (map (fn [[a b]]
         (Integer/parseInt b))
       (partition 2 rsp)))

(defn remove-key
  [key]
  (wcar* (car/del key)))

(defn add-rating
  "add user rating for item"
  [user, item, rating]
  (wcar*
    (car/zadd (str "user:" user ":items") rating item)
    (car/zadd (str "item:" item ":ratings") rating user)
    (car/zadd ("users" user))))

(defn get-user-items
  "return list of items rated by user"
  [user, max-items]
  (wcar* (car/zrevrange (str "user:" user ":items") 0 max-items)))

(defn get-user-suggestions
  "return list of user suggestions"
  [user, max-suggestions]
  (wcar* (car/zrevrange (str "user:" user ":suggestions") 0 max-suggestions "WITHSCORES")))

(defn get-item-ratings
  "return list rating an item has received"
  [item, max-ratings]
  (wcar*
    (car/zrevrange (str "item:" item ":ratings") 0 max-ratings)))

(defn calculate-user-similarity
  "calculates the RMS of the intersection between items rated by two users"
  [user1 user2]
  (let [users [(str "user:" user1 ":items") (str "user:" user2 ":items")]
        _ (wcar* (car/zinterstore* "ztmp" users "WEIGHTS" 1, -1))
        user-diffs (wcar* (car/zrange "ztmp" 0 -1 "WITHSCORES"))
        user-diffs-values (parse-redis-response-int user-diffs)]
    (remove-key "ztmp")
    (rms user-diffs-values)))

(defn calculate-item-probability
  "calculates average rating similar users gave an item"
  [user item]
  (let [user-item-keys [(str "user:" user ":similarusers") (str "item:" item ":ratings")]]
    (wcar* (car/zinterstore* "ztmp" user-item-keys "WEIGHTS" 0 1))

    (let [scores (wcar* (car/zrange "ztmp" 0 -1 "WITHSCORES"))
          ct (/ (count scores) 2)]
      (if (zero? ct)
        0
        (->> scores
             parse-redis-response-int
             (reduce +)
             (mean ct))))))

(defn get-similar-candidates
  "return vector of similar users based on item ratings"
  [user max]
  (let [items (get-user-items user max)
        item-ranking-sets (reduce (fn [coll item]
                                    (conj coll (str "item:" item ":scores")))
                                  []
                                  items)
        _ (wcar* (car/zunionstore* "ztmp" item-ranking-sets))
        union-users (wcar* (car/zrange "ztmp" 0 -1))]
    (wcar* (car/del "ztmp"))
    union-users))

(defn update-similar-users
  "updates the sorted set of similar users for each user"
  [max-sim-users]
  (let [users (wcar* (car/smembers "users"))]
    (if (empty? users)
      nil
      (do
        (map (fn [user]
               (let [candidates (get-similar-candidates user max-sim-users)
                     similar-users (flatten
                                     (reduce (fn [coll candidate]
                                               (conj coll [(calculate-user-similarity user candidate) candidate]))
                                             []
                                             candidates))]
                 (wcar*
                   (apply (partial car/zadd (str "user:" user ":similarusers")) similar-users)))) users)))))

(defn get-suggested-candidates
  "returns vector of similar users"
  [user max-candidates]
  (let [sim-users (wcar* (car/zrange (str "user:" user ":similarusers") 0 max-candidates))
        ct (count sim-users)
        user-item-sets (reduce (fn [coll sim-user]
                                 (conj coll (str "user:" sim-user ":items")))
                               []
                               sim-users)
        _ (wcar* (car/zunionstore* "ztmp" (join " " user-item-sets) "WEIGTHS" -1 "AGREGATE" "MIN"))
        union (car/zrangebyscore "ztmp" 0 "inf")]
    (car/del "ztmp")
    union))

(defn update-suggested-items
  "updates suggested items for user"
  [user max-items]
  (let [items (get-suggested-candidates user max-items)
        max-items (count items)
        add-items (flatten (reduce (fn [coll item]
                                     (conj coll [(calculate-item-probability user item) item]))
                                   []
                                   items))]
    (wcar*
      (apply (partial car/zadd (str "user:" user ":suggestions")) add-items))))