(ns js-time
  (:require
    [clojure.string :refer [join]]))

(defn ensure-leading [d]
  (join (take-last 2 (str "0" d))))

(defn js-date->simple-time [d]
  (str
    (ensure-leading (.getHours d))
    ":"
    (ensure-leading (.getMinutes d))
    ":"
    (ensure-leading (.getSeconds d))))

(defn simple-time []
  (let [d (js/Date.)]
    (js-date->simple-time d)))

(defn js-date->simple-date [d]
  (.slice
    (.toJSON
      d)
    0
    10))

(defn simple-date []
  (js-date->simple-date (js/Date.)))

(defn simple-datetime [d]
  (str
    (js-date->simple-date d)
    " "
    (js-date->simple-time d)))
