(ns app
  (:require
    [clojure.pprint :refer [pprint]]
    [clojure.string :as str]
    [clojure.edn :refer [read-string]]
    [reagent.core :as r]
    [reagent.dom :as rd]
    [cljs.core.async :refer [go]]
    [cljs.core.async.interop :refer-macros [<p!]]
    ;; 
    [router :as router :refer [go-to-page
                               ev-go-to-page]]
    [config :as config]
    [state :refer [app-state]]
    [api :as api]
    [pages.websites :refer [$websites]]
    [pages.login :refer [$login]]
    [pages.authenticate :refer [$authenticate]]
    [pages.domains :refer [$domains]]
    [pages.domain :refer [$domain]]
    [pages.home :refer [$home]]
    [pages.website :refer [$website]]
    [pages.profile :refer [$profile]]
    [pages.billing :refer [$billing]]
    [pages.billing-success :refer [$billing-success]]
    [pages.admin :refer [$admin]]
    [pages.admin-edit :refer [$admin-edit]]
    [scrutinize.app :as scrutinize]
    [ls :as ls]
    [auth :refer [redirect-if-no-token!
                  get-access-token
                  ls-get-token
                  access-token?]]
    [components.topbar :refer [$topbar]]))

(defn $billing-cancel []
  [:div
   [:div.block]
   [:div.block
     "Your purchase was cancelled"]
   [:a.button.is-info
    {:href "/billing"
     :onClick (ev-go-to-page app-state "/billing")}
    "Go to Billing"]])

(def handlers
  {:home $home
   :login $login
   :authenticate $authenticate
   :websites $websites
   :website $website
   :domains $domains
   :domain $domain
   :profile $profile
   :billing $billing
   :billing/cancel $billing-cancel
   :billing/success $billing-success
   :cameleon-deputies $admin
   :cameleon-deputies-edit $admin-edit
   :scrutinize/list scrutinize/$list
   :scrutinize/show scrutinize/$show})

(defn drop-index [col idx]
  (filter identity (map-indexed #(if (not= %1 idx) %2) col)))

(defn $notifications []
  [:div
    {:style {:position "fixed"
             :width "240px"
             :right 10
             :top 10
             :z-index 1000}}
    (map-indexed
      (fn [i n]
        [:div.notification
         {:class (get {:success "is-primary"
                       :warn "is-warning"
                       :failure "is-danger"}
                      (:type n))}
         [:button.delete
          {:onClick #(swap! app-state update :notifications drop-index i)}]
         [:div.content
           [:div
             (:text n)]
           [:div.has-text-weight-light
            "Happened at "
            (:date n)]]])
      (:notifications @app-state))])

(defn app []
  [:div
    [$notifications]
    [:div
     {:style {:position "fixed"
              :z-index 1000
              :opacity 0.1
              :top 0
              :left 8}}
     [:input
      {:type "checkbox"
       :onChange (ev-go-to-page app-state "/cameleon-deputies")}]]
    [:div
     {:style {:position "fixed"
              :z-index 1000
              :opacity 0.1
              :bottom 0
              :left 8}}
     [:input
      {:type "checkbox"
       :value (:debug? @app-state)
       :checked (:debug? @app-state)
       :onChange (fn [ev]
                   (let [v (-> ev .-target .-checked)]
                     (swap! app-state assoc :debug? v)
                     (ls/set! :debug? v)))}]]
    [:div.app.container
     ;; {:style {:font-family "Roboto, sans-serif"}}
     {:style {:max-width "1200px"}}
     [$topbar]
     (let [page (router/get-route handlers (:current-page @app-state))]
       (if (:element page)
         [(:element page) (:params page)]
         [:div "404"]))
     (when (:debug? @app-state)
       [:div
        [:button
         {:onClick (fn []
                     (let [current-page (-> @app-state :current-page)]
                       (go-to-page app-state "/")
                       (go-to-page app-state (-> @app-state :current-page))))}
         "Reload current page"]
        [:h3 "App State:"]
        [:pre (with-out-str (pprint @app-state))]])]])

(defn mount [el]
  (when (access-token?)
    (let [token (ls-get-token)]
      (swap! app-state assoc :tokens token)
      (api/get-me #(swap! app-state assoc :user (:user %)))))
  (router/setup-popstate-listener! app-state)
  (swap! app-state assoc :current-page (-> js/window
                                           .-location
                                           .-pathname))
  (rd/render [app] el))

(defn ^:dev/after-load init []
  (when-let [el (.getElementById js/document "root")]
    (mount el)))
