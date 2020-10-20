(ns page-component
  (:require
    [reagent.core :as r]
    [state :refer [app-state]]
    [auth :refer [access-token? access-token redirect-if-no-token!]]
    [api :refer [get-resource]]
    [clojure.string :as str]))

(def resource-mapping
  {:websites "websites"
   :website "websites/:id"
   :website-history "websites/:id/history"
   :domain "domains/:id"
   :profile "me"
   :everything "aaaaaaaaaaaaa/everything"
   :tx-logs "aaaaaaaaaaaaa/tx-logs"
   :recent-queries "aaaaaaaaaaaaa/recent-queries"
   :attribute-stats "aaaaaaaaaaaaa/attribute-stats"
   :entity "aaaaaaaaaaaaa/entity/:id"})

(defn fetch-resource [state namespace action]
  (if (nil? (get resource-mapping (first action)))
    (throw (js/Error. (str "[fetch-resource] Couldn't find action for resource " (first action))))
    (let [resource (first action)
          path (get resource-mapping resource)
          id (second action)
          full-path (str/replace path ":id" id)]
      (get-resource (access-token state)
                    full-path
                    (fn [res]
                      (swap! state assoc-in [namespace resource] res))))))

(comment
  (fetch-resource [:website "w3b19962dfe383bdc1347e44fbcdcc8bf"]))

(defn component [{:keys [to-render
                         namespace
                         resources]
                  :as opts}]
  (r/create-class
    {:reagent-render (fn [] to-render)
     :component-will-unmount
     (fn []
       (swap! app-state assoc namespace {}))
     :component-did-mount
     (fn []
       (redirect-if-no-token!)
       (when (access-token?)
         (swap! app-state assoc namespace {})
         (doseq [resource resources]
           (fetch-resource app-state namespace resource))))}))
