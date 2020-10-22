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
    [:a.website-list-item
     {:key (:crux.db/id website)
      :href (str "/websites/" (:crux.db/id website))
      :onClick (fn [ev]
                 (.preventDefault ev)
                 (when (.-target ev) (.-currentTarget ev)
                   (go-to-page app-state (str "/websites/" (:crux.db/id website)))))
      :style {:display "flex"
              :justify-content "space-between"
              :align-items "center"
              :margin 15
              :margin-left 0
              :padding 15
              :border-radius 3
              :background-color "rgba(246, 246, 246, 1)"
              :color "rgba(28, 28, 28, 1)"}}
     [:div
      {:style {:font-weight 500
               :font-size 18
               :width "20%"}}
      (:website/name website)]
     [:div
      {:style {:width "20%"
               :font-weight 400}}
      [:div
       {:style {:font-size 10
                :color "rgba(28, 28, 28, 1)"}}
       "15 minutes ago"]
      [:div
        {:style {:font-size 9
                 :color "rgba(154, 154, 154, 1)"}}
        "("
        (simple-datetime (:website/updated-at website))
        ")"]]
     [:div
      {:style {:font-size 10
               :font-weight "bolder"
               :width "20%"}}
      (if-let [domain (:website/domain website)]
        domain
        "No Domain Connected")]
     [:div
      {:style {:font-size 10}}
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
       ;; [:a.button.is-danger
       ;;  {:onClick (fn [ev]
       ;;              (.stopPropagation ev)
       ;;              (delete-website! (:crux.db/id website)
       ;;                               (fn [res]
       ;;                                 (fetch-resource app-state :page/websites [:websites])
       ;;                                 (notify! {:text "Website deleted!"
       ;;                                           :type :warn}))
       ;;                               (fn [err]
       ;;                                 (println "err")
       ;;                                 (.log js/console err))))}
       ;;  "Delete"]]]))

(defn $title []
  [:div
    {:style {:font-size 36
             :font-weight 300
             :color "rgba(28, 28, 28, 1)"
             :margin-left 15}}
    "Your Websites"])

(defn $searchbar []
  [:input
   {:style {:background "#FFFFFF"
            :border  "1px solid #C7C7C7"
            :box-sizing "border-box"
            :box-shadow "1px 1px 1px rgba(0, 0, 0, 0.07)"
            :border-radius "3px"
            :padding "5px"
            :font-size 14
            :margin-right 15}
    :placeholder "Search for websites here"
    :onChange (fn [ev]
                (swap! app-state
                       assoc-in
                       [:page/websites :search-term]
                       (-> ev .-target .-value .trim)))
    :value (-> @app-state :page/websites :search-term)
    :type "text"}])

(defn filter-by-term [websites term]
  (filter (fn [w]
            (let [name (lower-case (or (:website/name w) ""))
                  domain (lower-case (or (:website/domain w) ""))
                  term-lc (lower-case (or term ""))]
              (if term
                (or (includes? name term-lc)
                    (includes? domain term-lc))
                true)))
          websites))

(comment
  (def websites [{:website/name "Landing Page"
                  :website/domain nil}
                 {:website/name "Exit page"
                  :website/domain "exit.instantwebsite.app"}])
  (count
    (filter-by-term websites nil))
  ;; => 2
  (count
    (filter-by-term websites ""))
  ;; => 2
  (count
    (filter-by-term websites "Landing"))
  ;; => 1
  (count
    (filter-by-term websites "page"))
  ;; => 2
  (count
    (filter-by-term websites "instantwebsite")))
  ;; => 1

(defn -$websites []
  (if (empty? (-> @app-state :page/websites :websites))
    [how-to-get-websites-instructions]
    [:div
     {:style {:margin-top 20}}
     [:div
       {:style {:display "flex"
                :justify-content "space-between"
                :align-items "center"
                :margin-bottom 30}}
       [$title]
       [$searchbar]]
     [:div
       (let [websites (filter-by-term
                        (-> @app-state :page/websites :websites)
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

(defn $websites [{:keys []
                  :as opts}]
  (component
    {:to-render [-$websites]
     :namespace :page/websites
     :wait-for :websites
     :$loading $loading
     :resources [[:websites]]}))
