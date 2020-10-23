(ns components.searchbar
  (:require
    [clojure.string :refer [includes? lower-case]]))

(defn $searchbar [{:keys [value onChange placeholder]}]
  [:input
   {:style {:background "#FFFFFF"
            :border  "1px solid #C7C7C7"
            :box-sizing "border-box"
            :box-shadow "1px 1px 1px rgba(0, 0, 0, 0.07)"
            :border-radius "3px"
            :padding "5px"
            :font-size 14
            :margin-right 15}
    :placeholder placeholder
    :onChange onChange
    :value value
    :type "text"}])

(defn filter-by-term [websites ks term]
  (filter (fn [w]
            (let [matchers (map #(lower-case (or (% w) ""))
                                ks)
                  term-lc (lower-case (or term ""))]
              (if term
                (some (fn [i]
                        (includes? i term-lc))
                      matchers)
                true)))
          websites))
