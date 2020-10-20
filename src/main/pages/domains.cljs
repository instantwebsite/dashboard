(ns pages.domains
  (:require
    [reagent.core :as r]
    [auth :refer [redirect-if-no-token!]]
    [api :refer [fetch-domains-if-auth]]
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

(defn -$domains []
  [:div
   [$table {:heads ["Domain"
                    "Connected Website"
                    "Website Version"
                    "Auto Updating?"
                    "Verified?"]
            :items (:domains @app-state)
            :row-component $domain-row
            :new-url "/domains/new"
            :new-text "Create new domain"
            :empty-text "You have no domains."}]])

(defn $domains []
  (r/create-class
    {:reagent-render
     (fn []
       [-$domains])
     :component-did-mount
     (fn []
       (redirect-if-no-token!)
       (fetch-domains-if-auth))
     :component-will-unmount
     (fn []
       (swap! app-state assoc :domains #{}))}))
