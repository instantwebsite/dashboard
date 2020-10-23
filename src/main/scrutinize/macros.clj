(ns scrutinize.macros
  (:require
    [clojure.string :refer [includes?]]
    [clojure.java.io :refer [file]]))

(defn ls [dir]
  (file-seq
    (file dir)))

(defn filter-files [files]
  (map #(.getName %)
        (filter (fn [file]
                  (let [n (.getName file)]
                    (and
                      (= (take 5 (reverse n))
                         (reverse '(\. \c \l \j \s)))
                      (not (includes? (.getName file)
                                      "test")))))
                files)))

(comment
  (filter-files
    (ls "src/main/components")))

(defmacro list-components []
  (filter-files
    (ls "src/main/components")))

(defmacro docstring [symbol]
  `(:doc (meta (var ~symbol))))
