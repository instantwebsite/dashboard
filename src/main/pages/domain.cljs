(ns pages.domain
  (:require
    [clojure.pprint :refer [pprint]]
    [page-component :refer [component
                            fetch-resource]]
    [tick.alpha.api :as t]
    ;;
    [reagent.core :as r]
    ;;
    [state :refer [app-state]]
    [router :refer [go-to-page]]
    [notify :refer [notify!]]
    [auth :refer [redirect-if-no-token!]]
    [api :refer [fetch-domain-if-auth
                 create-domain!
                 verify-domain!
                 delete-domain!]]
    [components.label :refer [$label]]
    [components.text-input :refer [$text-input]]
    [page-component :refer [fetch-resource]]))

(defn assoc-new-domain [k v]
  (swap! app-state assoc-in [:page/domain :domain k] v))

(defn websites->options [websites]
  (concat
    [{:value nil
      :name "None"}]
    (map
      (fn [website]
        {:value (:crux.db/id website)
         :name (:website/name website)})
      websites)))

(defn format-time [ti]
  (t/format
    (tick.format/formatter "YYYY-MM-dd - hh:mm:ss")
    (t/date-time ti)))

(defn pretty-change [m]
  (str
    (format-time (:crux.db/valid-time m))
    " - "
    (subs (:crux.db/content-hash m) 0 8)))

(defn versions->options [history]
  (concat
    [{:value nil
      :name "None"}]
    (map
      (fn [item]
        {:value (:crux.db/content-hash item)
         :name (pretty-change item)})
      history)))

(defn start-loading! []
  (swap! app-state assoc-in [:page/domain :loading?] true))

(defn stop-loading! []
  (swap! app-state assoc-in [:page/domain :loading?] false))

(defn loading? []
  (-> @app-state :page/domain :loading?))

(defn set-invalid-values! [invalid-values]
  (swap! app-state assoc-in [:page/domain :invalid-values] invalid-values))

(defn invalid-value? [k]
  (some
    #(= % k)
    (-> @app-state :page/domain :invalid-values)))

(defn save-domain! [data]
  (start-loading!)
  (set-invalid-values! #{})
  (create-domain!
    data
    (fn [domain]
      (stop-loading!)
      (if (:crux.db/id data)
        (notify! {:type :success
                  :text "Domain saved"})
        (notify! {:type :success
                  :text "New domain created"}))
      (println "fetching domain")
      (fetch-resource app-state :page/domain [:domain (:crux.db/id domain)])
      (go-to-page app-state (str "/domains/" (:crux.db/id domain))))
    (fn [invalid-values]
      (set-invalid-values! invalid-values)
      (stop-loading!))))

;; TODO move to own component
(defn $checkbox [{:keys [label value onChange help disabled?]
                  :or {value false
                       disabled? false
                       onChange (fn [])}}]
  [:div.field
   [:div.control
    [:label.checkbox
      [:input
        {:type "checkbox"
         :disabled disabled?
         :onChange (fn [ev]
                     (onChange (-> ev .-target .-checked)))
         :checked value}]
      " "
      label]
    (when help
      [:p.help help])]])

;; TODO move to own component
(defn $select [{:keys [label options selected onChange help]}]
  (println "selected")
  (pprint selected)
  [$label
    {:label label
     :help help
     :input [:div.select
             [:select
               {:value (or selected "none")
                :defaultValue (or selected "none")
                :onChange (fn [ev]
                            (let [v (-> ev .-target .-value)]
                              (if (= v "None")
                                (onChange nil)
                                (onChange (-> ev .-target .-value)))))}
               (map (fn [{:keys [name value]}]
                      ^{:key value}
                      [:option {:value value} name])
                    options)]]}])

(defn fetch-website-versions [website-id]
  (fetch-resource app-state :page/domain [:website-history website-id]))

(defn $website-chooser [domain]
  (let [website-options (or (websites->options (-> @app-state :page/domain :websites)) [])
        version-options (or (versions->options (-> @app-state :page/domain :website-history)) [])]
    (println "options")
    (pprint version-options)
    (when (and
            (not (nil? (:domain/website-id domain)))
            (nil? (-> @app-state :page/domain :website-history)))
      (fetch-website-versions (:domain/website-id domain)))
    [:div.buttons
     [:div
      {:style (if (:domain/verified? domain)
                {}
                {:filter "blur(2px)"
                 :pointer-events "none"})}
      [$select {:label "Connected Website"
                :options website-options
                :help "What website should be shown when loading this domain"
                :onChange (fn [new-value]
                            (fetch-resource app-state :page/domain [:website-history new-value])
                            (assoc-new-domain :domain/website-id new-value))
                :selected (:domain/website-id domain)}]
      (when (:domain/website-id domain)
        [$select {:label "Website Version"
                  :options version-options
                  :help "Which website version to use"
                  :onChange (fn [new-value]
                              (assoc-new-domain :domain/website-revision new-value))
                  :selected (:domain/website-revision domain)}])]
     (when-not (:domain/verified? domain)
       [:div.notification.is-warning
        {:style {:margin-left 10}}
        "You need to verify your domain before you can connect a website to it"])]))

(defn -$domain [{:keys [id]}]
  (let [d (-> @app-state :page/domain :domain)
        existing-domain? (-> d :crux.db/id nil? not)]
    [:div.box
      [:h1.title
       (if existing-domain?
         "Edit"
         "Create new")
       " domain"]
      [:h1.subtitle (:domain/hostname d)]
      [:div.columns
        [:div.column.is-half
         {:style {:padding-top 0}}
         [$text-input {:label "Hostname"
                       :disabled? (or
                                    (loading?)
                                    existing-domain?)
                       :title (when (:domain/verified? d) "You cannot change the hostname of a verified domain")
                       :placeholder "mywebsite.example.com"
                       :help "Your domain name you want to use. Looks like \"mywebsite.example.com\""
                       :onEnter #(save-domain! d)
                       :invalid? (invalid-value? :domain/hostname)
                       :onChange (fn [new-value]
                                   (assoc-new-domain :domain/hostname new-value))
                       :value (:domain/hostname d)}]
         (when existing-domain?
           [$text-input {:label "Verification Code"
                         :disabled? true
                         :onChange (fn [new-value])
                         :help "Verification code you need to set as a TXT record for your domain"
                         :value (str "instantwebsite=" (:domain/verification-code d))}])
         (when existing-domain?
           [:div
            (when (and (:tried-verifying-domain? @app-state)
                       (not (:domain/verified? d)))
              [:div.notification
               "Make sure you have read the documentation on how to verify your
                domain and wait an hour or two if it still doesn't work. Domains
                (DNS really) is tricky like that..."])
            [:p.buttons
              [:button.button.is-info
               {:disabled (:domain/verified? d)
                :class (if (:verifying-domain? @app-state)
                         "is-loading")
                :onClick #(verify-domain! id (fn [new-domain]
                                               (swap! app-state
                                                      assoc-in
                                                      [:page/domain
                                                       :domain]
                                                      new-domain)))}
               "Verify Domain"]
              (if (:domain/verified? d)
                [:div.tag.is-success
                 "Your domain is verified!"]
                [:div.tag.is-warning
                 "You need to verify your domain before you can use it"])]])]
       [:div.column-is-half
         {:style {:padding-left 10}}
         [:div.block
           {:style {:margin-bottom 29}}
           [:label.label
             "Auto Update"]
           [$checkbox {:label [:span "Automatically select new website version when it gets uploaded"]
                       :help "Auto Update automatically sets new version of a website to the live version"
                       :disabled? (loading?)
                       :onChange (fn [new-value]
                                   (assoc-new-domain :domain/auto-update? new-value))
                       :value (:domain/auto-update? d)}]]
         (when existing-domain?
           [$website-chooser d])]]
      [:div.block
       [:div.block]
       [:div.block]
       [:div.block
         [:p.buttons
           [:button.button.is-primary
            {:onClick #(save-domain! d)
             :class (when (loading?) "is-loading")
             :disabled (loading?)}
            (if existing-domain?
              "Save"
              "Create")]]]]
      [:div.block]
      (when existing-domain?
        [:div.notification.is-danger.is-light
         [:h3.subtitle "Danger Zone"]
         ;; [:p.content]
         ;; [ "Be careful of these actions, they are irreversible!"]
         [:button.button.is-danger
          {:onClick #(delete-domain! id)}
          "Delete domain"]])
      (when (:debug? @app-state)
        [:div
         [:h3 "Domain"]
         [:pre (with-out-str (pprint d))]
         [:h3 "App State"]
         [:pre (with-out-str (pprint @app-state))]])]))

;; (defn $domain [{:keys [id]}]
;;   (r/create-class
;;     {:reagent-render
;;      (fn [props]
;;        [-$domain props])
;;      :component-did-update
;;      (fn []
;;        (println "commponent did update")
;;        (pprint (:new-domain @app-state))
;;        (when (-> @app-state :new-domain :domain/website-id)
;;          (println "Fetching website versions")))
;;      :component-did-mount
;;      (fn []
;;        (redirect-if-no-token!)
;;        (fetch-domain-if-auth id)
;;        (api/fetch-websites-if-auth)
;;        (println "component did mount")
;;        (pprint (:new-domain @app-state))
;;        (when (-> @app-state :new-domain :domain/website-id)
;;          (println "Fetching website versions")))
;;      :component-will-unmount
;;      (fn []
;;        (swap! app-state assoc :new-domain {}))}))

(defn $domain [{:keys [id]
                :as opts}]
  ^{:key id}
  [component
    {:to-render [-$domain opts]
     :namespace :page/domain
     :resources [[:websites]
                 [:domain id]]}])
