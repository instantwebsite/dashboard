(ns components.table
  (:require
    [reagent.core :as r]
    [state :refer [app-state]]
    [router :refer [ev-go-to-page]]))

(defn $table-row [{:keys [id url datas onMouseEnter onMouseLeave hoverAtom]
                   :or {hoverAtom (r/atom false)
                        onMouseEnter (fn [])
                        onMouseLeave (fn [])}}]
  [:a
   {:key id
    :onMouseEnter #(reset! hoverAtom true)
    :onMouseLeave onMouseLeave
    :style {:display "table-row"
            :vertical-align "inherit"
            :color "#363636"}
    :onClick (if url
               (ev-go-to-page app-state url)
               (fn []))
    :href url}
   (map (fn [d]
          ^{:key (str d)}
          [:td d])
        datas)])

(defn $table [{:keys [heads items row-component new-url new-text empty-text extra-class]}]
  [:div.box
    [:div
     [:div.block
       (when new-url
         [:a.button.is-info
           {:href new-url
            :onClick (ev-go-to-page app-state new-url)}
           new-text])]]
    [:div.block]
    [:div.block
      (if (empty? items)
        [:div empty-text]
        [:table.table.is-hoverable.is-fullwidth
          {:class extra-class}
          [:thead
           [:tr (map (fn [h]
                       [:th h])
                     heads)]]
          [:tbody
           (map (fn [data]
                  ^{:key (:crux.db/id data)}
                  [row-component data])
                items)]])]])

