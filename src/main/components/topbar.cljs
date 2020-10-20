(ns components.topbar
  (:require
    [state :refer [app-state]]
    [router :refer [go-to-page]]
    [auth :refer [reset-to-home!
                  access-token?]]
    [clojure.string :as str]
    [components.menu :refer [$menu]]))

(defn user->email [state]
  (-> state :user :user/email))

(defn user->plan [state]
  (-> state :user :user/plan))

(defn user->first-part [state]
  (-> state
      (user->email)
      (str/split "@")
      first))

(defn badge [title c]
  [:div.tag
   {:style {:height 20
            :width 30}
    :class c}
   title])

(def badges
  {:free [badge "free" "is-light"]
   :pro [badge "pro" "is-success"]})

(defn $topbar []
  [:nav.navbar
   {:role "navigation"
    :style {:margin-top 20}
    :aria-label "main navigation"}
   [:div.navbar-start
     [:div.navbar-item
      {:onClick #(go-to-page app-state "/")
       :style {:cursor "pointer"
               :font-weight "bold"}}
      [:img.image
       {:width 148
        :heigh 45
        :style {:max-height 45}
        :src "/img/logo.png"}]]]
   [:div.navbar-item
     (when (access-token?)
       [$menu (:current-page @app-state)])]
   [:div.navbar-end
     (when (-> @app-state :user :user/email)
       [:div.navbar-item
        {:style {:margin-right 20}
         :onClick #(go-to-page app-state "/profile")
         :title (user->email @app-state)}
        (user->first-part @app-state)
        [:span
         {:style {:margin-top -10
                  :margin-left 5}}
         (get badges (user->plan @app-state))]])]])
     ;; [:div.navbar-item
     ;;   (if (access-token?)
     ;;     [:a.button.is-text
     ;;      {:href "/logout"
     ;;       :onClick (fn [ev]
     ;;                  (.preventDefault ev)
     ;;                  (reset-to-home! app-state))}
     ;;      "Logout"]
     ;;     [:div.buttons
     ;;       [:a.button.is-dark
     ;;        {:style {}
     ;;         :href "https://instantwebsite.app"}
     ;;        "Homepage"]
     ;;       [:a.button.is-success
     ;;        {:href "/login"
     ;;         :onClick #(do
     ;;                     (.preventDefault %)
     ;;                     (go-to-page app-state "/login"))}
     ;;        "Signup / Login"]]))]])
