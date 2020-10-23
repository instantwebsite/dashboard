(ns pages.admin-edit
  (:require
    [reagent.core :as r]
    [ls :as ls]
    [clojure.pprint :as pprint]
    [notify :refer [notify!]]
    [page-component :refer [component
                            fetch-resource]]
    [clojure.edn :refer [read-string]]
    [clojure.string :as str]
    [router :refer [ev-go-to-page]]
    [api :as api]
    [components.table :refer [$table $table-row]]
    [state :refer [app-state]]
    [pages.admin :refer [$keys]]))

(defn start-loading! []
  (swap! app-state assoc-in [:page/admin-edit :loading?] true))

(defn stop-loading! []
  (swap! app-state assoc-in [:page/admin-edit :loading?] false))

(defn loading? []
  (-> @app-state :page/admin-edit :loading?))

(defn save-entity! [data]
  (start-loading!)
  (api/save-entity!
    data
    (fn [entity]
      (stop-loading!)
      (notify! {:type :success
                :text "Entity saved"})
      (swap! app-state assoc-in [:page/admin-edit :entity] entity))
      ;; (fetch-resource app-state :page/admin-edit [:entity (:crux.db/id entity)]))
    (fn [invalid-values]
      (stop-loading!))))

(defn $edit [t]
  (let [to-edit (r/atom t)]
    (fn [t]
      (pprint/pprint t)
      (pprint/pprint @to-edit)
      [:div
        [:textarea
         {:style {:width 1200
                  :height 600}
          :onChange (fn [ev]
                      (reset! to-edit (-> ev .-target .-value .trim read-string)))
          :value (with-out-str (pprint/pprint @to-edit))}]
        [:button.button.is-primary
         {:onClick #(save-entity! @to-edit)}
         "Save"]])))

(defn -$admin-edit []
  [:div
   [:div.block]
   [:div.block
     [:h1.title "Admin Editor"]]
   [:div.block
     (when-let [t (-> @app-state :page/admin-edit :entity)]
       [:div
         [$keys t]
         [$edit t]])]])

(defn $admin-edit [{:keys [id]
                    :as opts}]
  ^{:key id}
  [component
    {:to-render -$admin-edit
     :namespace :page/admin-edit
     :resources [[:entity id]]}])
