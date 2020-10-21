(ns notify
  (:require
    [js-time :refer [simple-time]]
    [state :refer [app-state]]))

(defn random-hex []
  (.toString (rand-int 16rFFFFFF) 16))

(comment
  (random-hex))

(defn filter-notifications [notifications id]
  (filter (fn [notification]
            (not= (:id notification) id))
          notifications))

(defn notify! [opts]
  (let [id (random-hex)]
    (swap! app-state update :notifications conj (merge {:id id
                                                        :type :success
                                                        :text "Remember I love you!"}
                                                       opts
                                                       {:date (simple-time)}))
    (.setTimeout
      js/window
      (fn []
        (swap! app-state update :notifications filter-notifications id))
      3000)))
