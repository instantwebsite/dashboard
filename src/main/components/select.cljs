(ns components.select
  (:require
    [components.label :refer [$label]]))

(defn $select [{:keys [label options selected onChange help]}]
  [$label
    {:label label
     :help help
     :input [:div.select
             [:select
               {:value (or selected "none")
                :defaultValue (or selected "none")
                :onChange (fn [ev]
                            (let [v (-> ev .-target .-value)]
                              (if (= v "None")
                                (onChange nil)
                                (onChange (-> ev .-target .-value)))))}
               (map (fn [{:keys [name value]}]
                      ^{:key value}
                      [:option {:value value} name])
                    options)]]}])
