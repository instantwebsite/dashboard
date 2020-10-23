(ns components.copy
  (:require
    [notify :refer [notify!]]))

(defn $copy [v]
  [:button.button
   {:onClick (fn []
               (.then
                 (.writeText
                   (.-clipboard js/navigator)
                   v)
                 #(notify! {:text "Copied to clipboard!"})))}
   "Copy"])

(def cases
  [["string" "Hello World"]
   ["nil" nil]
   ["bool" true]
   ["keyword" :hello]])
