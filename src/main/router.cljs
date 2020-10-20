(ns router
  (:require
    [bidi.bidi :as bidi]))

(def routes ["/" {"" :home
                  "login" :login
                  "authenticate" :authenticate
                  "websites" :websites
                  "domains" :domains
                  ["websites/" :id] :website
                  ["domains/" :id] :domain
                  "profile" :profile
                  "pricing" :billing
                  "pricing/cancel" :billing/cancel
                  "pricing/success" :billing/success
                  "cameleon-deputies" :cameleon-deputies
                  ["cameleon-deputies/" :id] :cameleon-deputies-edit}])

(defn get-route [handlers current-page]
  (let [k (bidi/match-route routes current-page)]
    {:element (get handlers (:handler k))
     :params (or (:route-params k) {})}))

(defn go-to-page [state new-url]
  (println (str "[go-to-page] " new-url))
  (.pushState
    js/history
    #{}
    ""
    new-url)
  (swap! state assoc :current-page new-url))

(defn ev-go-to-page [state page]
  (fn [ev]
    (.preventDefault ev)
    (go-to-page state page)))

(defn popstate-listener [state]
  (fn [ev]
    (go-to-page
      state
      (-> js/window
          .-location
          .-pathname))))

(defn setup-popstate-listener! [state]
  (.addEventListener
    js/window
    "popstate"
    (popstate-listener state)))

