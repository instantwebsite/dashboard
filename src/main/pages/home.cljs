(ns pages.home
  (:require
    [state :refer [app-state]]
    [auth :refer [access-token]]
    [router :refer [go-to-page
                    ev-go-to-page]]))

(defn $home []
  (if (access-token app-state)
    (go-to-page app-state "/websites")
    (go-to-page app-state "/login"))
  [:div
    [:div "We're redirecting you to the right place"]
    [:div
     "Please "
     [:a
       {:href "/login"
        :onClick (ev-go-to-page app-state "/login")}
       "login"]]])

