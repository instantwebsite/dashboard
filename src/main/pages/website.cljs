(ns pages.website
  (:require
    [clojure.pprint :refer [pprint]]
    [page-component :refer [component]]
    [clojure.string :refer [join]]
    [config :refer [config]]
    [router :refer [ev-go-to-page
                    go-to-page]]
    [state :refer [app-state]]
    [components.table :refer [$table $table-row]]
    [components.text-input :refer [$text-input]]
    [components.select :refer [$select]]
    [api :refer [http]]
    [tick.alpha.api :as t]
    [tick.locale-en-us :as l]))

;; to be extracted
;; [] buttons
;; [] formatting

(defn format-time [ti]
  (t/format
    (tick.format/formatter "YYYY-MM-dd - HH:mm:ss")
    (t/date-time ti)))

(comment
  (t/date-time #inst "2020-10-02T21:05:06.822-00:00")
  (format-time #inst "2020-10-02T21:05:06.822-00:00"))

(defn construct-visit-url [website-id content-hash]
  (str (:website-preview config)
       website-id
       "/"
       content-hash))

(defn download-blob [blob filename]
  (let [url (.createObjectURL js/window.URL blob)
        a-tag (.createElement js/document "a")]
    (set! (.-href a-tag) url)
    (set! (.-download a-tag) (str filename ".zip"))
    (.appendChild js/document.body a-tag)
    (.click a-tag)
    (.remove a-tag)))

(defn handle-download [website-id content-hash]
  (let [path (str "websites/"
                  website-id
                  "/"
                  content-hash
                  "/exports/html")]
    (http {:path path
           :method :get
           :response-type :blob}
          (fn [res]
            (download-blob res content-hash))
          (fn [res]
            (println "download failure")))))

(defn $input-group [label, input]
  [:div
   [:div label]
   [:div input]])

(defn $label [v]
  [:div
   {:style {:font-size 18
            :font-weight 300
            :color "#1C1C1C"
            :letter-spacing "0px"}}
   v])

(defn $view-item [{:keys [title url alt]}]
  [:a.page-list-item
   {:href url
    :title alt
    :target "_blank"
    :style {:display "flex"
            :justify-content "space-between"
            :align-items "center"
            :max-width "300px"
            :font-weight 300}}
   [:div
     {:style {:font-size 14
              :color "rgba(28, 28, 28, 1)"
              :font-weight 300}}
     title]
   [:div
    {:style {:font-size 12
             :font-weight 300}}
    "View"]])

(defn $page-list-item [page website-id latest-version]
  [$view-item {:title (:page/title page)
               :url (str (construct-visit-url website-id latest-version)
                         "/"
                         (:page/path page))}])

(defn $history-item-title [history]
  [:div
   [:span
    {:style {:display "inline-block"
             :width 153}}
    (format-time (:crux.db/valid-time history))]
   [:code
    "("
    (join
      ""
      (take 5 (:crux.db/content-hash history)))
    ")"]])

(defn $history-list-item [history website-id]
  [$view-item {:title [$history-item-title history]
               :alt (:crux.db/content-hash history)
               :url (str (construct-visit-url website-id (:crux.db/content-hash history)))}])

(defn $page-list [pages website-id latest-version]
  [:<>
    (map (fn [[path page]]
           ^{:key path}
           [$page-list-item page website-id latest-version])
         pages)])

(defn $history-list [histories website-id]
  [:div
   {:style {:max-height 300
            :overflow-y "scroll"}}
   (map (fn [history]
          ^{:key (:crux.db/content-hash history)}
          [$history-list-item history website-id])
        histories)])

(defn pages->options [pages]
  (map
    (fn [[page-path page]]
      {:value page-path
       :name (:page/title page)})
    pages))


(defn -$website [{:keys [resources
                         reset-mutations
                         save-mutations
                         saving-mutation?
                         loading?
                         mutator]}]
  (let [website (:website resources)
        user-pro? (= (-> @app-state :user :user/plan) :pro)
        edit-website! (mutator :website)
        saving-website? (saving-mutation? :website)
        latest-version (-> @app-state
                           :page/website
                           :website-history
                           first
                           :crux.db/content-hash)]
    [:div.iw-box
      {:style {:margin-top 30
               :opacity (if (loading?) 0.5 1)}}
      [:div.iw-title
       (if (loading?)
         "-"
         (:website/name website))]
      [:div
        {:style {:display "flex"
                 :justify-content "space-between"
                 :align-items "top"
                 :margin-top 30}}
        [:div
          {:style {:width "33%"
                   :margin-right 30}}
          [:div
            [$input-group
             [$label "Website Name"]
             [$text-input
              {:onChange #(edit-website! :website/name %)
               :value (:website/name website)}]]]
          [:div
            {:style {:margin-top 30}}
            [$input-group
             [$label "Description"]
             [$text-input
              {:onChange #(edit-website! :website/description %)
               :value (:website/description website)}]]]
          [:div
            {:style {:margin-top 30}}
            [$input-group
             [$select {:label [$label "Start Page"]
                       :options (pages->options (:website/pages website))
                       :onChange (fn [new-value]
                                   (edit-website! :website/startpage new-value))
                       :selected (:website/startpage website)}]]]

          [:div
            {:style {:margin-top 30}}
            [$input-group
             [$label "Connected Domain"]
             [:div
              (if-let [domain (:website/domain website)]
                [:div
                  {:style {:font-weight 600
                           :font-size 14}}
                  (:domain/hostname domain)
                  [:div
                   {:style {:margin-top 5
                            :font-size 10}}
                   [:a
                    {:href (str "https://" (:domain/hostname domain))
                     :target "_blank"
                     :style {:margin-right 10}}
                    "Visit"]
                   (let [edit-url (str "/domains/" (:crux.db/id domain))]
                     [:a
                      {:href edit-url
                       :onClick (ev-go-to-page app-state edit-url)}
                      "Edit"])]]
                "None")]]]]
        [:div
          {:style {:width "33%"
                   :margin-right 30}}
          [$label
           "Pages"]
          [:div
           [$page-list (:website/pages website)
                       (:crux.db/id website)
                       latest-version]]]
        [:div
          {:style {:width "33%"}}
          [$label
           "Website Versions"]
          [$history-list (butlast (-> @app-state :page/website :website-history))
                         (:crux.db/id website)]]]
      [:div.buttons
       {:style {:margin-top 100
                :display "flex"
                :justify-content "space-between"
                :align-items "center"}}
       [:div
         [:a.button
          {:onClick #(save-mutations :website)
           :disabled (saving-website?)
           :style {:background-color "#35C2E1"
                   :border "none"
                   :color "white"}}
          "Save"]
         [:a.button
          {:onClick #(reset-mutations :website)
           :disabled (saving-website?)
           :style {:background-color "lightgrey"
                   :border "none"
                   :color "#333"}}
          "Reset"]
         [:a.button
          {:style {:background-color "#E66EBF"
                   :border "none"
                   :color "white"}
           :title (if user-pro?
                    "Downloads the latest version of your website as a ZIP file. You can then extract and upload this file wherever you want!"
                    "You need to be subscribed to Instant Website Pro in order to download websites")
           :disabled (not user-pro?)
           :onClick (fn []
                      (if user-pro?
                        (handle-download
                          (:crux.db/id website)
                          latest-version)
                        (go-to-page app-state "/pricing")))}
          "Download Latest Version"]]
       [:div
         [:a.button
          {:style {:background-color "#E2674C"
                   :border "none"
                   :color "white"}}
          "Delete"]]]]))

(defn $website [{:keys [id]
                 :as opts}]
  (component
    {:to-render -$website 
     :args opts
     :wait-for :website-history
     :namespace :page/website
     :resources [[:website id]
                 [:website-history id]]}))
