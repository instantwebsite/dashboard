(ns notify
  (:require
    [clojure.string :refer [join]]
    [state :refer [app-state]]))

(defn ensure-leading [d]
  (join (take-last 2 (str "0" d))))

(defn simple-time []
  (let [d (js/Date.)]
    (str
      (ensure-leading (.getHours d))
      ":"
      (ensure-leading (.getMinutes d))
      ":"
      (ensure-leading (.getSeconds d)))))

(defn random-hex []
  (.toString (rand-int 16rFFFFFF) 16))

(comment
  (random-hex))

(defn simple-date []
  (.slice
    (.toJSON
      (js/Date.))
    0
    10))

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
