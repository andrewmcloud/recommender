(ns recommender.core-test
  (:require [clojure.test :refer :all]
            [recommender.core :refer :all]))

(deftest simple-integration-test
  (testing "Recommender picks most similar first."
    (clear-db)
    (rate "name1" "i1" 3)
    (rate "name1" "i2" 3)
    (rate "name1" "i3" 2)
    (rate "name2" "i3" 5)
    (rate "name2" "i1" 1)
    (rate "name2" "i4" 6)
    (rate "name3" "i1" 3)
    (rate "name3" "i2" 3)
    (rate "name3" "i5" 7)
    (update-db 5)
    (is (= ["i5" "7" "i4" "6"] (suggest "name1" 5)))))
