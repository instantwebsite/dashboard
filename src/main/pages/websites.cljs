(ns pages.websites
  (:require
    [clojure.string :refer [includes? lower-case]]
    [reagent.core :as r]
    [state :refer [app-state]]
    [auth :refer [redirect-if-no-token!]]
    [api :refer [fetch-websites-if-auth
                 delete-website!]]
    [notify :refer [notify!]]
    [router :refer [ev-go-to-page
                    go-to-page]]
    [components.table :refer [$table $table-row]]
    [components.searchbar :refer [$searchbar
                                  filter-by-term]]
    [page-component :refer [component fetch-resource]]
    [config :refer [config]]
    [js-time :refer [simple-datetime]]
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

(defn $website-row [website]
  (fn [website]
    [:a.iw-box.iw-list-item
     {:key (:crux.db/id website)
      :href (str "/websites/" (:crux.db/id website))
      :onClick (fn [ev]
                 (.preventDefault ev)
                 (when (.-target ev) (.-currentTarget ev)
                   (go-to-page app-state (str "/websites/" (:crux.db/id website)))))}
     [:div.iw-list-item-title
      (:website/name website)]
     [:div.iw-list-item-timestamp
      [:div.iw-list-item-timestamp-ago
       "15 minutes ago"]
      [:div.iw-list-item-timestamp-full
        "("
        (simple-datetime (:website/updated-at website))
        ")"]]
     [:div.iw-list-item-related
      (if-let [domain (:website/domain website)]
        [:span
          {:style {:font-weight 500}}
          domain]
        [:span
          {:style {:font-weight 400}}
          "No Domain Connected"])]
     [:div.iw-list-item-counter
      (let [c (count (:website/pages website))]
        (condp = c
          0 "No pages"
          1 "1 page"
          (str c " pages")))]
     [:div
       {:style {:display "flex"
                :width "100px"
                :justify-content "space-between"
                :align-items "center"
                :font-size 14
                :margin-right 30}}
       [:a.link
        {:href (str "/websites/" (:crux.db/id website))
         :onClick (ev-go-to-page app-state (str "/websites/" (:crux.db/id website)))
         :target "_blank"}
        "Edit"]
       [:a.link
        {:href (str (:website-preview config)
                    (:crux.db/id website))
         :onClick #(.stopPropagation %)
         :target "_blank"}
        "Visit"]]]))

(defn $title []
  [:div.iw-title
    {:style {:margin-left 15}}
    "Your Websites"])

(defn onSearchChange [ev]
  (swap! app-state
         assoc-in
         [:page/websites :search-term]
         (-> ev .-target .-value)))

(defn -$websites []
  (if (empty? (-> @app-state :page/websites :websites))
    [how-to-get-websites-instructions]
    [:div
     [:div
       {:style {:display "flex"
                :justify-content "space-between"
                :align-items "center"
                :margin-bottom 30}}
       [$title]
       [$searchbar {:onChange onSearchChange
                    :placeholder "Search for websites here"
                    :value (-> @app-state :page/websites :search-term)}]]
     [:div
       (let [websites (filter-by-term
                        (-> @app-state :page/websites :websites)
                        [:website/name :website/domain]
                        (-> @app-state :page/websites :search-term))]
         (if (empty? websites)
           [:div
            (str "No results for search term '"
                 (-> @app-state :page/websites :search-term)
                 "'")]
           (map (fn [w]
                  ^{:key (:crux.db/id w)}
                  [$website-row w])
                websites)))]]))


(defn $loading []
  [:div
   {:style {:margin-top 20}}
   [$title]
   [:div
    {:style {:text-align "center"}}
    "Loading"]])

(defn $websites [opts]
  (component
    {:to-render -$websites
     :namespace :page/websites
     :wait-for :websites
     :$loading $loading
     :resources [[:websites]]}))
