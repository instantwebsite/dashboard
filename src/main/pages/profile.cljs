(ns pages.profile
  (:require
    [reagent.core :as r]
    [clojure.pprint :refer [pprint]]
    [page-component :refer [component]]
    [state :refer [app-state]]
    [components.text-input :refer [$text-input]]
    [auth :refer [reset-to-home!]]
    [components.copy :refer [$copy]]))

(defn -extract [{{email :user/email} :user
                 {api :tokens/api
                  plugin :tokens/plugin} :tokens}]
  {:email email
   :api api
   :plugin plugin})

(defn state->me [state]
  (-> state
      :page/profile
      :profile
      (-extract)))

(defn -$profile []
  (let [billing? (r/atom false)]
    (fn []
      (let [me (state->me @app-state)]
        [:div
         [:div.block]
         [:h1.subtitle
          "Your Profile"]
         [$text-input
          {:label "Email"
           :disabled? true
           :value (:email me)}]
         [$text-input
          {:label "Figma Plugin Token"
           :disabled? true
           :value (:plugin me)
           :style {:width 431}
           :addon [$copy (:plugin me)]}]
         ;; Why did we put API token here? 
         ;; [$text-input
         ;;  {:label "API Token"
         ;;   :disabled? true
         ;;   :value (:api me)
         ;;   :style {:width 431}
         ;;   :addon [$copy (:api me)]}]
         [:div.block]
         [:p.buttons
           [:button.button.is-danger
            {:onClick (fn [ev]
                        (.preventDefault ev)
                        (reset-to-home! app-state))}
            "Logout"]
           [:button.button.is-primary
            {:class (when @billing? "is-loading")
             :onClick (fn [ev]
                        (reset! billing? true)
                        (.preventDefault ev)
                        (api/create-portal-session
                          (fn [err res]
                            (pprint [err res])
                            (set!
                              js/window.location.href
                              (:url res)))))}
            "Manage Billing Settings"]]]))))

(defn $profile []
  (component
    {:to-render -$profile
     :namespace :page/profile
     :resources [[:profile]]}))
