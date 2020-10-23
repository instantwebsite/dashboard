(ns pages.domains
  (:require
    [reagent.core :as r]
    [auth :refer [redirect-if-no-token!]]
    [api :refer [fetch-domains-if-auth]]
    [page-component :refer [component]]
    [components.searchbar :refer [$searchbar
                                  filter-by-term]]
    [state :refer [app-state]]
    [components.table :refer [$table $table-row]]))

(defn $domain-row [domain]
  [$table-row {:id (:crux.db/id domain)
               :url (str "/domains/" (:crux.db/id domain))
               :datas [(:domain/hostname domain)
                       (or (:domain/website-id domain) "None")
                       [:div
                         {:title (:domain/website-revision domain)}
                         (subs
                           (or (:domain/website-revision domain) "None")
                           0 8)]
                       (if (:domain/auto-update? domain)
                         "Yes"
                         "No")
                       (if (:domain/verified? domain)
                         [:span.tag.is-success
                          "Verified"]
                         [:span.tag.is-warning
                          "Missing Verification"])]}])

(defn $title []
  [:div.iw-title
    {:style {:margin-left 15}}
    "Your Domains"])

(defn onSearchChange [ev]
  (swap! app-state
         assoc-in
         [:page/domains :search-term]
         (-> ev .-target .-value .trim)))

(defn -$domains [{:keys [resources]}]
  (let [domains (:domains resources)]
    [:div
     [:div
       {:style {:display "flex"
                :justify-content "space-between"
                :align-items "center"
                :margin-bottom 30}}
       [$title]
       [$searchbar {:onChange onSearchChange
                    :placeholder "Search for domains here"
                    :value (-> @app-state :page/domains :search-term)}]]
     [$table {:heads ["Domain"
                      "Connected Website"
                      "Website Version"
                      "Auto Updating?"
                      "Verified?"]
              :items domains
              :row-component $domain-row
              :new-url "/domains/new"
              :new-text "Create new domain"
              :empty-text "You have no domains."}]]))

(defn $domains [opts]
  (component
    {:to-render -$domains
     :namespace :page/domains
     :wait-for :domains
     :resources [[:domains]]}))
