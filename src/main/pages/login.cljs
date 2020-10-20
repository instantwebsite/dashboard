(ns pages.login
  (:require
    [reagent.core :as r]
    [clojure.pprint :refer [pprint]]
    [state :refer [app-state]]
    [auth :refer [do-login]]))

(def description
  "InstantWebsite.app uses passwordless login, so once you've submitted your
  email, we'll send you a link you can use once to your inbox for completing the login")

(defn $login []
  (let [user-email (r/atom "")
        submitting? (r/atom false)
        submitted? (r/atom false)
        error (r/atom nil)
        reset (fn []
                (reset! submitting? false)
                (reset! submitted? false)
                (reset! error nil))]
    (fn []
      [:section.section
       [:div.container
         [:h1.title
          "Login"]
         [:div.columns
           [:div.column
             [:p
              {:style {:width "500px"}}
              description]]
           [:div.column
             (if (and @submitted?
                      (nil? @error))
             ;; (if true
               [:div
                 [:div.notification.is-info
                  [:div.block
                   [:h1.title.is-5
                    "Check your email '"
                    [:strong
                      @user-email]
                    "' for the login code"]]
                  [:div.block
                    "You can now close this tab. Email contains link for login"]
                  [:div.block
                   "If the email doesn't arrive within 30 seconds, check your
                   spam directory. If nothing works, you can "
                   [:a
                     {:onClick #(do (reset)
                                    (do-login @user-email submitting? submitted? error))}
                     " request another email"]]]]
               [:div
                 [:div
                   [:h3.has-text-weight-bold "Your Email"]
                   [:div.field.has-addons
                     [:div.control
                       [:input.input
                        {:type "email"
                         :class (when @error "is-danger")
                         :value @user-email
                         :onKeyUp
                         (fn [ev]
                           (let [keyCode (-> ev .-keyCode)
                                 enter? (= keyCode 13)]
                             (reset)
                             (when enter?
                               (do-login @user-email submitting? submitted? error))))
                         :onChange #(reset! user-email
                                            (-> % .-target .-value))
                         :placeholder "bob@example.com"}]]
                     [:submit.button
                      {:disabled @submitting?
                       :class (if @submitting?
                                "is-loading"
                                (when @error
                                  "is-danger"))
                       :onClick #(do-login @user-email submitting? submitted? error)}
                      "Send Code"]]
                   (when @error
                     [:div.notification.is-danger
                      "Something went wrong. Please try again later"])]
                 [:div.block]
                 (when (:debug? @app-state)
                   [:div
                    [:strong
                      "Dev Emails"]
                    (map (fn [email]
                           ^{:key email}
                            [:div
                              [:a
                               {:onClick #(reset! user-email email)}
                               email]])
                         ["victorbjelkholm@gmail.com"
                          "bjelkholm@aol.com"
                          "instantwebsite@victor.earth"
                          "v@instantwebsite.app"
                          "demo@instantwebsite.app"])])])]]]])))
