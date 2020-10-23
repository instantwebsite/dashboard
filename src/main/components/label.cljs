(ns components.label)

(defn $label
  "$label shows a <label/> element, probably next to an <input/> element"
  [{:keys [label addon input help invalid?]}]
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

(def cases
  [["empty args" {}]
   ["with label" {:label "Hello There"}]
   ["with addon" {:label "Hello There"
                  :addon [:div "Ima addon"]}]
   ["with help" {:label "Hello There"
                 :help "Here you get help"
                 :addon [:div "Ima addon"]}]
   ["with input" {:label "Hello There"
                  :help "Here you get help"
                  :input [:input {:placeholder "Type here, I dare you"}]
                  :addon [:div "Ima addon"]}]
   ["is invalid" {:label "Hello There"
                  :help "Here you get help"
                  :invalid? true
                  :input [:input {:placeholder "Type here, I dare you"}]
                  :addon [:div "Ima addon"]}]])
