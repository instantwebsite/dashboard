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

(defn $website-row2 [website]
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
              :color "#363636"
              :margin 15
              :padding 15
              :border-radius 3
              :background-color "rgba(246, 246, 246, 1)"
              ::color "rgba(28, 28, 28, 1)"}}
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

(defn $website [{:keys [title updated-at domain pages state]}]
  [:div
   {:style {:background-color "rgba(246, 246, 246, 1)"
            :color "rgba(28, 28, 28, 1)"}}
   [:div
    {:style {:font-family "Inter"
             :font-weight 500
             :font-size "18px"}}
    title]
   [:div updated-at]
   [:div domain]])

(comment
  (defn $websites []
    (redirect-if-no-token!)
    (when (-> @app-state :page/websites :websites nil?)
      (fetch-resource app-state :page/websites [:websites]))
    [:div
     ;; [$website
     ;;  {:title "Landing Page"}
     [:div
      (if (empty? (-> @app-state :page/websites :websites))
        [how-to-get-websites-instructions]
        (map (fn [w]
               ^{:key (:crux.db/id w)}
               [$website-row2 w])
             (-> @app-state :page/websites :websites)))]]))
        ;; [$table {:heads ["Name" "Pages" "Active?" ""]
        ;;          :items (-> @app-state :page/websites :websites)
        ;;          :row-component $website-row2)))]]))

(defn $title []
  [:div
    {:style {:font-size 36
             :font-weight 300
             :color "rgba(28, 28, 28, 1)"
             :margin-left 15
             :margin-bottom 30}}
    "Your Websites"])

(defn -$websites []
  (if (empty? (-> @app-state :page/websites :websites))
    [how-to-get-websites-instructions]
    [:div
     {:style {:margin-top 20}}
      ;; [$website
      ;;  {:title "Landing Page"
      ;;   :updated-at "2014-04-13 23:32:23"
      ;;   :domain "beta.instantwebsite.app"
      ;;   :pages 4
      ;;   :state :active
      ;;   :id "w1203123"}
     [$title]
     [:div
       (map (fn [w]
              ^{:key (:crux.db/id w)}
              [$website-row2 w])
            (-> @app-state :page/websites :websites))]]))

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
