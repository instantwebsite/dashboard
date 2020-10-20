(ns ls
  (:refer-clojure :exclude [get])
  (:require
    [clojure.edn :refer [read-string]]))

(defn set! [k v]
  (.setItem js/localStorage k (prn-str v)))

(defn get [k]
  (read-string (.getItem js/localStorage k)))

(defn del! [k]
  (.removeItem js/localStorage k))
