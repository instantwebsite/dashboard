(ns components.label)

(defn $label [{:keys [label
                      addon
                      input
                      help
                      invalid?]}]
  [:div.field
    [:label.label
     label]
    [:div.field
      {:class (when addon "has-addons")}
      [:div.control
        {:style {:max-width 500}}
        input]
      (if invalid?
        [:div.tag.is-danger
         help]
        (when help
          [:p.help
           help]))
      (when addon
        [:div.control
         addon])]])
