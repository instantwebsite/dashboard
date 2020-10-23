(ns components.text-input
  (:require
    [components.label :refer [$label]]))

;; TODO move to own component
(defn $text-input [{:keys [label value onChange
                           disabled? title placeholder
                           addon help style onEnter
                           invalid?]
                    :or {onEnter (fn [])
                         invalid? false}}]
  [$label
    {:label label
     :addon addon
     :help help
     :invalid? invalid?
     :input [:input.input
             {:type "text"
              :value value
              :placeholder placeholder
              :class (when invalid? "is-danger")
              :disabled disabled?
              :style style
              :title title
              :onKeyUp (fn [ev]
                         (when (= (.-keyCode ev) 13)
                           (onEnter (-> ev .-target .-value))))
              :onChange (fn [ev]
                          (onChange (-> ev .-target .-value)))}]}])

