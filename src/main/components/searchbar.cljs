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

(defn filter-by-term [websites term]
  (filter (fn [w]
            (let [name (lower-case (or (:website/name w) ""))
                  domain (lower-case (or (:website/domain w) ""))
                  term-lc (lower-case (or term ""))]
              (if term
                (or (includes? name term-lc)
                    (includes? domain term-lc))
                true)))
          websites))

(comment
  (def websites [{:website/name "Landing Page"
                  :website/domain nil}
                 {:website/name "Exit page"
                  :website/domain "exit.instantwebsite.app"}])
  (count
    (filter-by-term websites nil))
  ;; => 2
  (count
    (filter-by-term websites ""))
  ;; => 2
  (count
    (filter-by-term websites "Landing"))
  ;; => 1
  (count
    (filter-by-term websites "page"))
  ;; => 2
  (count
    (filter-by-term websites "instantwebsite")))
  ;; => 1
