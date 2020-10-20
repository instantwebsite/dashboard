(ns pages.billing
  (:require
    [reagent.core :as r]
    [clojure.pprint :refer [pprint]]
    [page-component :refer [component]]
    [api :refer [create-checkout-session!]]
    [state :refer [app-state]]))

(defn -extract [{{email :user} :identity
                 {api :token/api
                  plugin :token/plugin} :token}]
  {:email email
   :api api
   :plugin plugin})

(def test-stripe-key "pk_test_51HSMvQKE4zmFNORm65J9SLL5msUzVVzCsyvyLgsgb3H1AbzPO5y70H85b1r6Bir3I4kVdvB4JgPI3wTVMpShlVAy00cGdCRDQQ")
(def prod-stripe-key "pk_live_51HSMvQKE4zmFNORmFFXSafjBbCXP25h0R5HfvOTP79tw6DNaA7VtZLHc7QwLqmfDVlB9yLRr4d3fQgVhdDOh2gjn00gMSSWQWs")

;; Force CLJS to dead-code elimate non-active paths by forcing it to be a boolean
(def debug? ^js/boolean goog.DEBUG)

(defn add-stripe! [callback]
  (let [script (.createElement js/document "script")]
    (set! (.-src script)
          "https://js.stripe.com/v3")
    (set! (.-onload script)
          (fn []
            (callback
              (js/Stripe (if debug?
                           test-stripe-key
                           prod-stripe-key)))))
    (.appendChild
      (.querySelector js/document "head")
      script)))

(defn <email [state]
  (-> state :page/profile :profile :user :user/email))

(defn handle-checkout []
  (swap! app-state assoc :checkout-loading? true)
  (add-stripe!
    (fn [^js/Object stripe]
      (create-checkout-session!
        (fn [err res]
          (println "[create-checkout-session!] " (:id res))
          (pprint res)
          (let [session-id (:id res)]
            (.redirectToCheckout
              stripe
              #js {:sessionId session-id})))))))

(defn $help [content extra-class]
  [:div.tag.is-rounded
   {:class extra-class
    :style {:margin-left 3
            :height 18
            :width 10
            :line-height "17px"
            :cursor "help"}
    ;;:style {:display "inline-block"
    ;;        :width 15
    ;;        :font-size 12
    ;;        :height 15
    ;;        :background-color "black"
    ;;        :text-align "center"
    ;;        :line-height "15px"
    ;;        :border-radius 15
    ;;        :font-weight 1000
    ;;        :color "white"
    ;;        :cursor "help"
    ;;        :margin-left 2
    :title content}
   "?"])

(def plans
  {:free {:name "Free Beta"
          :price "€0"
          :whitelabeled "No"
          :export "None"
          :websites 3
          :protect "None"}
   :pro {:name "Pro Beta"
         :price "€10"
         :whitelabeled "Yes"
         :export ".zip archive"
         :websites "Unlimited"
         :protect "Password"}})

(defn $plan [{:keys [plan-key
                     notification-type]}]
  (let [plan (get plans plan-key)
        class-mod (if (= plan-key :pro)
                    "is-info is-light"
                    "is-info")]
    [:div.column.is-two-fifths
     [:div.notification
      {:class notification-type}
      [:h1.title.is-2.has-text-centered
        (:name plan)]
      [:div.block]
      [:h1.subtitle.is-3.has-text-centered
        {:style {:margin-left 60}}
        (:price plan)
        [:span
         {:style {:display "inline"
                  :font-size 16
                  ;; :background-color "orange"
                  :width 60}}
         " / month"]]
      [:div.columns.is-centered.is-vcentered
       [:div.column.has-text-right
        "White Labeled"
        [$help "If every website created has a tiny little banner in the bottom left" class-mod]]
       [:div.column
        [:div.title.is-4
          (:whitelabeled plan)]]]
      [:div.columns.is-centered.is-vcentered
       [:div.column.has-text-right
        "Max Websites"
        [$help "How many websites can max exist in your account" class-mod]]
       [:div.column
        [:div.title.is-4
          (:websites plan)]]]
      ;; [:div.columns.is-centered.is-vcentered
      ;;  [:div.column.has-text-right
      ;;   "Export (soon)"
      ;;   [$help "If the builds of the websites can be exported from InstantWebsite. We're still working on this feature. If you need it right away, contact us" class-mod]]
      ;;  [:div.column
      ;;   [:div.title.is-4
      ;;     (:export plan)]]]
      ;; [:div.columns.is-centered.is-vcentered
      ;;  [:div.column.has-text-right
      ;;   "Protection (soon)"
      ;;   [$help "How you can limit who can access your website. We're still working on this feature. If you need it right away, contact us" class-mod]]
      ;;  [:div.column
      ;;   [:div.title.is-4
      ;;     (:protect plan)]]]
      ;; [:div.columns.is-centered.is-vcentered
      ;;  [:div.column.has-text-right
      ;;   "Support"
      ;;   [$help "What support channels you'll have access to"]]
      ;;  [:div.column
      ;;   [:div.title.is-4
      ;;    (:support plan)]]]
      [:div.columns.is-centered.is-vcentered
       [:div.column.is-centered.is-one-third
         [:button#checkout-button.button.is-success
          {:onClick handle-checkout
           :style {:opacity (if (= plan-key :pro) 1 0)}
           :class (when (:checkout-loading? @app-state)
                    "is-loading")}
          "Subscribe"]]]]]))

(defn -$billing []
 [:div.block
   {:style {:margin-top 100}}
   [:div.columns.is-centered
    [$plan
     {:plan-key :free
      :notification-type "is-info is-light"}]
    [$plan
     {:plan-key :pro
      :notification-type "is-info"}]]])

(defn $billing []
  (component
    {:to-render [-$billing]
     :namespace :page/profile
     :resources [[:profile]]}))
