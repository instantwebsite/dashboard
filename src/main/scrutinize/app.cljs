(ns scrutinize.app
  (:require
    [clojure.pprint :refer [pprint]]
    [scrutinize.macros :refer-macros [docstring]]
    [state :refer [app-state]]
    [router :refer [ev-go-to-page]]
    [components.copy :as copy]
    [components.label :as label]))

(def components
  {"components.copy/$copy" [components.copy/$copy
                            components.copy/cases]
   "components.label/$label" [components.label/$label
                              components.label/cases]})

(defn $show [{:keys [selector]
              :as opts}]
  (if-let [found-component (get components selector)]
    [:div
     {:style {:margin-top 50}}
     [:div.iw-title
       "Scrutinizing "
       [:code selector]]
     (let [c (first found-component)
           cases (second found-component)]
       (map (fn [[title arg]]
              [:div
               [:div
                {:style {:margin-top 50
                         :font-size 20}}
                "Case "
                [:code title]]
               [:div
                 {:style {:border "1px solid rgba(0,0,0,0.5)"
                          :padding 25
                          :margin-top 10}}
                 [c arg]]
               [:div "ARG"]
               [:pre
                (with-out-str (pprint arg))]])
            cases))]
    [:div
     "Didn't find any components with the name "
     selector]))

(defn $list [opts]
  [:div
   {:style {:margin-top 50}}
   [:div.iw-title
     "Scrutinizing All Components"]
   [:ul
     (map (fn [[title func]]
            (pprint func)
            (let [url (str "/scrutinize/" title)]
              [:li
               [:a
                {:onClick (ev-go-to-page app-state url)
                 :href url}
                (str title)]]))
          components)]])
