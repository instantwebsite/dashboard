(ns page-component
  (:require
    [reagent.core :as r]
    [state :refer [app-state]]
    [auth :refer [access-token? access-token redirect-if-no-token!]]
    [api :refer [get-resource]]
    [notify :refer [notify!]]
    [clojure.edn :refer [read-string]]
    [clojure.string :as str]))

(def resource-mapping
  {:websites "websites"
   :website "websites/:id"
   :website-history "websites/:id/history"
   :domains "domains"
   :domain "domains/:id"
   :profile "me"
   :everything "aaaaaaaaaaaaa/everything"
   :tx-logs "aaaaaaaaaaaaa/tx-logs"
   :recent-queries "aaaaaaaaaaaaa/recent-queries"
   :attribute-stats "aaaaaaaaaaaaa/attribute-stats"
   :entity "aaaaaaaaaaaaa/entity/:id"})

(defn fetch-resource
  ([state namespace action]
   (fetch-resource state namespace action (fn [])))
  ([state namespace action on-done]
   (if (nil? (get resource-mapping (first action)))
     (throw (js/Error. (str "[fetch-resource] Couldn't find action for resource " (first action))))
     (let [resource (first action)
           path (get resource-mapping resource)
           id (second action)
           full-path (str/replace path ":id" id)]
       (get-resource (access-token state)
                     full-path
                     (fn [res]
                       (swap! state assoc-in [namespace resource] res)
                       (swap! state assoc-in [namespace "_reserve" resource] res)
                       (on-done res)))))))

(defn create-or-save [resource new-data]
  (let [path (get resource-mapping resource)]
    (if (:crux.db/id new-data)
      {:path (str/replace path ":id" (:crux.db/id new-data))
       :method :put}
      {:path path
       :method :post})))

(defn save-resource [state namespace resource on-done]
  (let [resource-to-save (-> @state namespace resource)
        d (create-or-save resource resource-to-save)]
    (api/http {:path (:path d)
               :method (:method d)
               :edn resource-to-save
               :parse-fn read-string}
              (fn [body]
                (swap! state assoc-in [namespace resource] body)
                (on-done)
                (notify! {:text "Saved!"
                          :type :success}))
              (fn [err]
                (.error js/console err)))))

(defn $demo-loading []
  [:div "Loading"])

(comment
  (fetch-resource [:website "w3b19962dfe383bdc1347e44fbcdcc8bf"]))

(comment
  (:page/website @app-state))

(defn fetch-resources [state namespace resources loaded? wait-for]
  (doseq [resource resources]
    (if (= (first resource) wait-for)
      (fetch-resource state namespace resource
                      #(reset! loaded? true))
      (fetch-resource state namespace resource))))

(defn component [{:keys [to-render
                         namespace
                         resources
                         wait-for
                         $loading]
                  :as opts}]
  (let [loaded? (r/atom (nil? wait-for))
        saving-states (r/atom {})
        reset (fn [to-reset]
                (let [s (get @app-state namespace)
                      current (get s to-reset)
                      previous (get-in s ["_reserve" to-reset])]
                  (if (= current previous)
                    (notify! {:text "Nothing to reset..."})
                    (swap! app-state assoc-in [namespace to-reset]
                           (-> @app-state
                               namespace
                               (get "_reserve")
                               to-reset)))))
        save (fn [to-save]
               (swap! saving-states assoc to-save true)
               (save-resource app-state
                              namespace
                              to-save
                              (fn []
                                (println "saved")
                                (swap! saving-states assoc to-save false)
                                (fetch-resources
                                  app-state
                                  namespace
                                  resources
                                  loaded?
                                  wait-for))))
        saving-mutation? (fn [query]
                           (fn []
                             (get @saving-states query)))
        mutator (fn [to-edit]
                  (fn [property new-value]
                    (swap! app-state assoc-in [namespace to-edit property] new-value)))]
    (r/create-class
      {:reagent-render (fn []
                         [:div
                          {:style {:margin-top 15}}
                          (let [args {:resources (namespace @app-state)
                                      :reset-mutations reset
                                      :loading? (fn [] (not @loaded?))
                                      :save-mutations save
                                      :saving-mutation? saving-mutation?
                                      :mutator mutator}]
                           (if $loading
                             (if @loaded?
                               [to-render args]
                               [$loading])
                             [to-render args]))])
       :component-will-unmount
       (fn []
         (swap! app-state assoc namespace {}))
       :component-did-mount
       (fn []
         (redirect-if-no-token!)
         (when (access-token?)
           (swap! app-state assoc namespace {})
           (fetch-resources app-state namespace resources loaded? wait-for)))})))
