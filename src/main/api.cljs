(ns api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [state :refer [app-state]]
    [config :refer [config]]
    [auth :refer [reset-to-home!
                  access-token]]
    [router :refer [go-to-page]]
    [notify :refer [notify!]]
    ;;
    [clojure.pprint :refer [pprint]]
    [clojure.edn :refer [read-string]]
    [cljs.core.async :refer [<!]]
    [cljs.core.async.interop :refer-macros [<p!]]
    [cljs-http.client :as http]))

(comment
  (read-string {:readers {'crux/id identity}}
               "{:id #crux/id \"hello\"}")

  (read-string "{:id \"hello\"}"))

(defn fetch-with-auth [path access-token]
  (.fetch
    js/window
    (str (:api config) path)
    #js{:headers #js{"Authorization" (str "Token " access-token)}}))

(defn catch-forbidden [err]
  (if (= (.-message err) "403")
    (reset-to-home! app-state)
    (do
      (println "Got uncaught error")
      (println (.-message err))
      (.log js/console err))))

(defn text-if-ok [res]
  (if (.-ok res)
    (.text res)
    (throw (js/Error. (.-status res)))))

(defn get-websites [access-token callback]
  (go
    (try
      (let [req (<p! (fetch-with-auth "websites" access-token))
            text (<p! (text-if-ok req))
            data (read-string text)]
        (callback data))
      (catch js/Error err
        (catch-forbidden err)))))

(defn get-domains [access-token callback]
  (go
    (try
      (let [req (<p! (fetch-with-auth "domains" access-token))
            text (<p! (text-if-ok req))
            data (read-string text)]
        (callback data))
      (catch js/Error err
        (catch-forbidden err)))))

(defn get-domain [access-token id callback]
    (->
      (.fetch
        js/window
        (str (:api config) "domains/" id)
        #js{:headers #js{"Authorization" (str "Token " access-token)}})
      (.then (fn [res] (.text res)))
      (.then (fn [text] (callback (read-string text))))))

(defn fetch-websites-if-auth []
  (when (and (not (:fetched-websites? @app-state))
             (access-token app-state))
    (println "had access token")
    (swap! app-state assoc :fetched-websites? true)
    (get-websites (access-token app-state)
                  (fn [websites]
                    (swap! app-state assoc :websites websites)))))

(defn fetch-domains-if-auth []
  (when (access-token app-state)
    (get-domains (access-token app-state)
                 (fn [domains]
                   (swap! app-state assoc :domains domains)))))

(defn fetch-domain-if-auth [id]
  (when (access-token app-state)
    (swap! app-state assoc :fetched-domain? true)
    (println "getting domain")
    (api/get-domain (access-token app-state)
                    id
                    (fn [domain]
                      (swap! app-state assoc :domain domain)
                      (swap! app-state assoc :new-domain domain)))))

(defn create-or-save [new-data]
  (if (:crux.db/id new-data)
    {:path (str "domains/" (:crux.db/id new-data))
     :method :put}
    {:path "domains"
     :method :post}))

(defn get-params [opts]
  (merge
    (if (:edn opts)
      {:edn-params (:edn opts)}
      {})
    opts))

(defn get-headers [opts]
  {"Authorization" (str "Token " (access-token app-state))})

(defn http [opts on-success on-failure]
  (go (let [params (get-params opts)
            parse-fn (or (:parse-fn opts) identity)
            method (or (:method opts) :get)
            http-fn (condp = method
                      :get http/get
                      :put http/put
                      :post http/post
                      :delete http/delete)
            full-url (or (:url opts) (str (:api config/config) (:path opts)))
            response (<! (http-fn full-url
                                  (merge params
                                         {:headers (get-headers opts)})))]
        (println (str "[http "
                      method
                      " => "
                      full-url
                      "] response status " (:status response)))
        (if (or (= (:status response) 200)
                (= (:status response) 201))
          (on-success (parse-fn (:body response)))
          (if (= (:status response) 0)
            (on-failure "Failed to connect to server")
            (on-failure (parse-fn (:body response))))))))

(defn get-invalid-values-from-error [res]
  (into #{}
        (flatten
          (map :in
               (-> res :message :clojure.spec.alpha/problems)))))

(defn create-domain! [new-data on-success on-failure]
  (let [d (create-or-save new-data)]
    (http {:path (:path d)
           :method (:method d)
           :edn new-data
           :parse-fn read-string}
          (fn [body]
            (on-success body))
          (fn [body]
            (on-failure (get-invalid-values-from-error body))))))

(defn delete-website! [website-id on-success on-failure]
  (http {:path (str "websites/" website-id)
         :method :delete}
        (fn [body]
          (on-success body))
        (fn [err]
          (on-failure err))))

(defn get-me
  ([on-success]
   (get-me on-success (fn [err]
                        (notify! {:text err
                                  :type :failure}))))
  ([on-success on-failure]
   (http {:path "me"}
         (fn [body]
           (on-success body))
         (fn [err]
           (on-failure err)))))

(defn save-entity! [new-data on-success on-failure]
  (http {:path (str "aaaaaaaaaaaaa/entity/" (:crux.db/id new-data))
         :method :put
         :edn new-data
         :parse-fn read-string}
        (fn [body]
          (on-success body))
        (fn [body]
          (on-failure body))))

(defn save-domain! [new-data]
  (let [d (create-or-save new-data)]
    (->
      (.fetch
        js/window
        (str (:api config/config) (:path d))
        #js{:headers #js{"Authorization" (str "Token " (access-token app-state))
                         "Content-Type" "application/edn"}
            :method (:method d)
            :body (prn-str new-data)})
      (.then (fn [res] (.text res)))
      (.then (fn [body]
               (read-string body)))
      (.then (fn [domain]
               (fetch-domain-if-auth (:crux.db/id domain))
               (go-to-page app-state (str "/domains/" (:crux.db/id domain)))
               (if (:crux.db/id new-data)
                 (notify! {:type :success
                           :text "Domain saved"})
                 (notify! {:type :success
                           :text "New domain created"})))))))

(defn delete-domain! [id]
  (->
    (.fetch
      js/window
      (str (:api config/config) (str "domains/" id))
      #js{:headers #js{"Authorization" (str "Token " (access-token app-state))}
          :method "DELETE"})
    (.then (fn [res] (.text res)))
    (.then (fn [body]
             (read-string body)))
    (.then (fn [domain]
             (notify! {:type :warn
                       :text "Domain was deleted!"})
             (go-to-page app-state "/domains")))))

(defn verify-domain! [id on-success]
  (swap! app-state assoc :verifying-domain? true)
  (http {:path (str "domains/" id "/verify")
         :method :post
         :parse-fn read-string}
        (fn [body]
          (swap! app-state assoc :verifying-domain? false)
          (swap! app-state assoc :tried-verifying-domain? true)
          (fetch-domain-if-auth id)
          (on-success body))
        (fn [body]
          (let [err (str body)]
            (swap! app-state assoc :verifying-domain? false)
            (notify! {:text err
                      :type :failure})))))

(defn create-checkout-session! [callback]
  (->
    (.fetch
      js/window
      (str (:api config/config) (str "checkout-session"))
      #js{:headers #js{"Authorization" (str "Token " (access-token app-state))}
          :method "POST"})
    (.then (fn [res] (.text res)))
    (.then (fn [body]
             (callback nil (read-string body))))))

(defn create-portal-session [callback]
  (->
    (.fetch
      js/window
      (str (:api config/config) (str "portal-session"))
      #js{:headers #js{"Authorization" (str "Token " (access-token app-state))}
          :method "POST"})
    (.then (fn [res] (.text res)))
    (.then (fn [body]
             (callback nil (read-string body))))))

(defn get-resource [access-token resource callback]
  (go
    (try
      (let [req (<p! (fetch-with-auth resource access-token))
            text (<p! (text-if-ok req))
            data (read-string {:readers {'crux/id identity}}
                              text)]
        (callback data))
      (catch js/Error err
        (catch-forbidden err)))))
