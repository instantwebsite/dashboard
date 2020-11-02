(ns pages.authenticate
  (:require
    [clojure.string :as str]
    [clojure.pprint :refer [pprint]]
    ;;
    [reagent.core :as r]
    ;;
    [auth :refer [get-token
                  get-access-token
                  persist-token!]]
    [api :as api]
    [notify :refer [notify!]]
    [state :refer [app-state]]
    [router :refer [go-to-page ev-go-to-page]]))

(defn location-search []
  (.-search js/location))

(defn return-to []
  (if (= (location-search) "")
    nil
    (last
      (str/split (location-search)
                 #"="))))

(defn $authenticate []
  (let [error (r/atom nil)]
    (r/create-class
      {:component-did-mount
       (fn []
         (let [auth (str/split (str/join "" (rest (-> js/window .-location .-hash))) #"/")
               email (first auth)
               login-code (second auth)]
           (get-token email
                       login-code
                       (fn [err profile]
                         (if err
                           (reset! error err)
                           (let [token (select-keys profile [:tokens/api :tokens/plugin])]
                             (swap! app-state assoc :tokens token)
                             (persist-token! token)
                             (swap! app-state assoc :username email)
                             (api/get-me (fn [res]
                                           (swap! app-state assoc :user (:user res))
                                           (if (return-to)
                                             (go-to-page app-state (return-to))
                                             (go-to-page app-state "/websites"))))))))
           (println "Grabbing token and getting access/refresh token")))
       :render
       (fn []
         (if @error
           [:div
            {:style {:width "500px"
                     :margin "50px auto"}}
            [:div
              "Something went wrong. Could be that the token expired or we're having some temporary issue. Please try again,
               or contact support@instantwebsite.app if the problem persists."]
            [:div.block]
            [:a.button.is-info
             {:href "/login"
              :onClick (ev-go-to-page app-state "/login")}
             "Back to login"]]
           [:div "Hold on, authenticating you now..."]))})))
