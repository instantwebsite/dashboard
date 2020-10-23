(ns components.searchbar-test
  (:require
    [cljs.test :refer [deftest testing is]]
    [components.searchbar :refer [filter-by-term]]))

(def websites [{:website/name "Landing Page"
                :website/domain nil}
               {:website/name "Exit page"
                :website/domain "exit.instantwebsite.app"}])

(def website-keys [:website/name :website/domain])

(def domains [{:domain/hostname "first.domain"}
              {:domain/hostname "second.domain"}])

(def domain-keys [:domain/hostname])

(defn search-websites [term]
  (filter-by-term websites website-keys term))

(defn search-domains [term]
  (filter-by-term domains domain-keys term))

(deftest filter-by-term-test
  (testing "filtering websites"
    (let [cases [[nil 2 websites]
                 ["" 2 websites]
                 ["Landing" 1 [(first websites)]]
                 ["page" 2 websites]
                 ["instantwebsite.app" 1 [(last websites)]]]]
      (doseq [[arg wanted-count wanted-results] cases]
        (let [res (search-websites arg)]
          (is (= wanted-count (count res)))
          (is (= wanted-results res))))))
  (testing "filtering domains"
    (let [cases [[nil 2 domains]
                 ["" 2 domains]
                 ["first" 1 [(first domains)]]
                 [".domain" 2 domains]
                 ["second" 1 [(last domains)]]]]
      (doseq [[arg wanted-count wanted-results] cases]
        (let [res (search-domains arg)]
          (is (= wanted-count (count res)))
          (is (= wanted-results res)))))))
