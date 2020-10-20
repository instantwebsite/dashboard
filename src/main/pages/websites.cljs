(ns pages.websites
  (:require
    [reagent.core :as r]
    [state :refer [app-state]]
    [auth :refer [redirect-if-no-token!]]
    [api :refer [fetch-websites-if-auth
                 delete-website!]]
    [notify :refer [notify!]]
    [router :refer [ev-go-to-page
                    go-to-page]]
    [components.table :refer [$table $table-row]]
    [page-component :refer [fetch-resource]]
    [config :refer [config]]
    ;;
    [clojure.pprint :refer [pprint]]))

(defn how-to-get-websites-instructions []
  [:section.hero.is-primary.is-medium
   [:div.hero-body
    [:div.container
     [:h1.title
      "You have no websites yet"]
     [:br]
     [:h2.subtitle
      [:div
        "Install the "
        [:a
         {:style {:text-decoration "underline"
                  :font-weight 700}}
         "Figma Plugin"]
        " to get started"]
      [:br]
      [:div
       "Use the \"Figma Plugin Token\" from the "
        [:a
         {:href "/profile"
          :onClick (ev-go-to-page app-state "/profile")
          :style {:text-decoration "underline"
                  :font-weight 700}}
         "Profile Page"]
       " to connect with Figma"]
      [:br]
      [:div
        "Take a look at the "
        [:a
         {:style {:text-decoration "underline"
                  :font-weight 700}}
         "Documentation"]
        " if you need more help"]]]]])

(defn $website-row2 [website]
  (let [showing? (r/atom false)]
    (fn [website]
      [:a
       {:key (:crux.db/id website)
        :onMouseEnter #(reset! showing? true)
        :onMouseLeave #(reset! showing? false)
        :onClick (fn [ev]
                   (when (.-target ev) (.-currentTarget ev)
                     (go-to-page app-state (str "/websites/" (:crux.db/id website)))))
        :style {:display "table-row"
                :vertical-align "inherit"
                :color "#363636"}}
       [:td (:website/name website)]
       [:td (count (:website/pages website))]
       [:td "Active"]
       [:td
         [:p.buttons
           {:style {:opacity (if @showing? 1 0)}}
           [:a.button.is-info
            {:href (str "/websites/" (:crux.db/id website))
             :onClick (ev-go-to-page app-state (str "/websites/" (:crux.db/id website)))
             :target "_blank"}
            "Edit"]
           [:a.button.is-success
            {:href (str (:api config) "websites/" (:crux.db/id website))
             :onClick #(.stopPropagation %)
             :target "_blank"}
            "Visit Website"]
           [:a.button.is-danger
            {:onClick (fn [ev]
                        (.stopPropagation ev)
                        (delete-website! (:crux.db/id website)
                                         (fn [res]
                                           (fetch-resource app-state :page/websites [:websites])
                                           (notify! {:text "Website deleted!"
                                                     :type :warn}))
                                         (fn [err]
                                           (println "err")
                                           (.log js/console err))))}
            "Delete"]]]])))

(defn $websites []
  (redirect-if-no-token!)
  (when (-> @app-state :page/websites :websites nil?)
    (fetch-resource app-state :page/websites [:websites]))
  [:div
   [:div
    (if (empty? (-> @app-state :page/websites :websites))
      [how-to-get-websites-instructions]
      [$table {:heads ["Name" "Pages" "Active?" ""]
               :items (-> @app-state :page/websites :websites)
               :row-component $website-row2}])]])
