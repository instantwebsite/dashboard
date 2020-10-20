(ns auth
  (:require
    [clojure.edn :refer [read-string]]
    [clojure.pprint :refer [pprint]]
    ;;
    [router :refer [go-to-page]]
    [state :refer [app-state]]
    [notify :refer [notify!]]
    [config :as config]))

(defn access-token [state]
  ;; (println "Getting access token from state")
  ;; (pprint @state)
  ;; (pprint (-> @state :token :token/api))
  (-> @state :token :token/api))

(defn current-path []
  (.-pathname js/location))

(defn redirect-if-no-token! []
  (pprint (access-token app-state))
  (when (not (access-token app-state))
    (println "didnt have the access token...")
    (go-to-page app-state (str "/login?return-to=" (current-path)))))

(defn remove-access-token! []
  (.removeItem js/localStorage "token"))

(defn ls-set! [k v]
  (.setItem js/localStorage k (prn-str v)))

(defn ls-get [k]
  (read-string (.getItem js/localStorage k)))

(defn persist-token! [token]
  (ls-set! "token" token))

(defn ls-get-token []
  (ls-get "token"))

(defn get-access-token []
  (:token/api (ls-get-token)))

(defn access-token? []
  (not (empty? (get-access-token))))

(defn reset-to-home! [state]
  (remove-access-token!)
  (swap! state assoc :token {})
  (swap! state assoc :username nil)
  (swap! state assoc :user nil)
  (.setTimeout
    js/window
    #(go-to-page state "/")
    100)
  (notify! {:type :info
            :text "You've been logged out!"}))

(defn location-search []
  (.-search js/location))

(defn do-login [email submitting? submitted? error]
  (reset! submitting? true)
  (->
    (.fetch
      js/window
      (str (:api config/config) "login/email/" email (location-search))
      #js{:method "post"})
    (.catch (fn [err]
              (reset! error err)))
    (.then #(do
              (reset! submitting? false)
              (reset! submitted? true)))))

(defn get-token [email login-code cb]
  (-> (.fetch
        js/window
        (str (:api config/config) "login/email/" email "/code/" login-code)
        #js{:method "POST"})
      (.then
        (fn [res]
          (.text res)))
      (.then
        (fn [text]
          (cb (read-string text))))))
