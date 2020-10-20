(ns components.menu
  (:require
    [clojure.string :refer [starts-with?]]
    [state :refer [app-state]]
    [router :refer [ev-go-to-page]]))

(defn menu-item-style [active?]
  {:color "#356AA0"
   :transition "opacity 0.2s, text-decoration 0.2s"
   :opacity (if active? 1 0.6)
   :text-decoration (if active?
                      "underline"
                      "none")})

(defn $menu-item [name path current-page extra-style]
  (let [active? (starts-with? current-page path)]
    [:a.navbar-item
      {:style {:text-decoration (if active? "underline" "inherit")}
       :class (when active? "is-active")
       ;; :style (merge (menu-item-style (= path current-page))
       ;;               extra-style
       :href path
       :onClick (ev-go-to-page app-state path)}
      name]))

(defn $menu [current-page]
  [:div.navbar-menu
   {:style {:display "flex"}}
   [$menu-item "Websites" "/websites" current-page {:margin-right 20}]
   [$menu-item "Domains" "/domains" current-page {:margin-right 20}]
   (when (= (-> @app-state :user :user/plan)
            :free)
     [$menu-item "Pricing" "/pricing" current-page {}])
   [$menu-item "My Profile" "/profile" current-page {}]])
