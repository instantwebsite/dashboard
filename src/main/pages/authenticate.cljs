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
    [router :refer [go-to-page]]))

(defn location-search []
  (.-search js/location))

(defn return-to []
  (if (= (location-search) "")
    nil
    (last
      (str/split (location-search)
                 #"="))))

(defn $authenticate []
  (r/create-class
    {:component-did-mount
     (fn []
       (let [auth (str/split (str/join "" (rest (-> js/window .-location .-hash))) #"/")
             email (first auth)
             login-code (second auth)]
         (get-token email
                     login-code
                     (fn [profile]
                       (let [token (select-keys profile [:token/api :token/plugin])]
                         (pprint token)
                         (swap! app-state assoc :token token)
                         (persist-token! token)
                         (swap! app-state assoc :username email)
                         (api/get-me (fn [res]
                                       (swap! app-state assoc :user (:user res))
                                       (if (return-to)
                                         (go-to-page app-state (return-to))
                                         (go-to-page app-state "/websites")))))))
         (println "Grabbing token and getting access/refresh token")))
     :render
     (fn []
       [:div "Hold on, authenticating you now..."])}))
