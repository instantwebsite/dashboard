(ns pages.billing-success
  (:require
    [reagent.core :as r]
    [page-component :refer [component]]
    [router :refer [ev-go-to-page]]
    [api :refer [create-checkout-session!]]
    [auth :refer [get-access-token]]
    [state :refer [app-state]]))

(defn watch-has-pro-plan? [callback]
  (.setInterval
    js/window
    (fn []
      (println "checking if watch-has-pro-plan?")
      (api/get-me
        (fn [res]
          (swap! app-state assoc :user (:user res))
          (println (= :pro (-> res :user :user/plan)))
          (when (= :pro (-> res :user :user/plan))
            (callback true)))))
    3000))

(defn -$billing-success []
  (let [loading? (r/atom false)
        watch (r/atom nil)]
    (r/create-class
      {:component-will-unmount
       (fn []
         (when @watch
           (.clearInterval
             js/window
             @watch)))
       :component-did-mount
       (fn []
         (reset! loading? true)
         (reset! watch (watch-has-pro-plan? #(reset! loading? false))))
       :reagent-render
       (fn []
         [:div
          [:div.block]
          [:h1.subtitle
           (if @loading?
             "Validating payment..."
             "Payment successful!")]
          [:a.button
           {:onClick (ev-go-to-page app-state "/profile")
            :href "/billing"
            :class (if @loading?
                     "is-loading is-info"
                     "is-primary")}
           "Go to your profile"]])})))

(defn $billing-success []
  (component
    {:to-render -$billing-success
     :namespace :page/billing-success
     :resources [[:profile]]}))
