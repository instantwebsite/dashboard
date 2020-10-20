(ns pages.admin
  (:require
    [reagent.core :as r]
    [ls :as ls]
    [clojure.pprint :as pprint]
    [page-component :refer [component]]
    [clojure.string :as str]
    [router :refer [ev-go-to-page]]
    [api :refer [create-checkout-session!]]
    [components.table :refer [$table $table-row]]
    [state :refer [app-state]]))

(defn ensure-leading [d]
  (str/join (take-last 2 (str "0" d))))

(defn simple-time [d]
  (str
    (ensure-leading (.getHours d))
    ":"
    (ensure-leading (.getMinutes d))
    ":"
    (ensure-leading (.getSeconds d))))

(defn simple-date [d]
  (.slice
    (.toJSON
      d)
    0
    10))

(defn datetime [d]
  (str (simple-time d)
       " "
       (simple-date d)))


(defn $tx-log-row [{:keys [crux.tx/tx-id
                           crux.tx/tx-time
                           crux.api/tx-ops]}]
  [:tr
   [:td tx-id]
   [:td
    {:style {:width 110}}
    (datetime tx-time)]
   [:td
    [:pre
     {:style {:max-width 600
              :max-height 300}}
     (with-out-str (pprint/pprint tx-ops))]]])

(defn map-vec->keys [v]
  (reduce
    (fn [acc curr]
      (into #{} (concat acc (map str (keys curr)))))
    #{}
    v))

(defn is-eid? [id]
  (when (= (count id) 33)
    (-> (get
          {"i" "image"
           "c" "login-code"
           "e" "vector"
           "t" "token"
           "u" "user"
           "p" "page"
           "w" "website"
           "d" "domain"}
          (first id))
        (nil?)
        (not))))

(defn id->type [id]
  (pprint/pprint id)
  (if (string? id)
    (get
      {"i" "image"
       "c" "login-code"
       "e" "vector"
       "t" "token"
       "u" "user"
       "p" "page"
       "w" "website"
       "d" "domain"}
      (first id)
      "unknown")
    "unkown"))

(defn date? [d]
  (= js/Date (type d)))

(defn linked-id [id]
  [:a.button.is-link
   {:href (str "/cameleon-deputies/" id)
    :onClick (ev-go-to-page app-state (str "/cameleon-deputies/" id))}
   id])

(comment
  (count "w1796e8304bf4752b08d56d47a8ad23a9"))

(defn $keys [m]
  [:div.notification
   [:div.title.is-5
    (id->type (:crux.db/id m))
    " - "
    (:crux.db/id m)]
   (map
     (fn [k]
       [:div.columns.is-gapless
        [:div.column.is-one-fifth
          [:div.title.is-6
           (str k)]]
        [:div.column
          {:title (with-out-str (pprint/pprint (k m)))}
          (let [v (k m)]
            (condp apply [v]
              boolean? [:span.tag
                        {:class (if v
                                  "is-success"
                                  "is-danger")}
                        (if v
                          "true"
                          "false")]
              nil? [:span.tag.is-dark "nil"]
              keyword? [:span.tag.is-info
                        (str v)]
              date? [:span (str v)]
              is-eid? [:span [linked-id v]]
              [:div (-> v
                        (str)
                        (subs 0 100))]))]])
          ;; (if (boolean? (k m))
          ;;   (if (k m)
          ;;     [:span.tag.is-success
          ;;       "true"]
          ;;     [:span.tag.is-danger
          ;;       "false"])
          ;;   (if (nil? (k m))
          ;;     [:span.tag.is-dark
          ;;      "nil"]
          ;;     (if (keyword? (k m))
          ;;       [:span.tag.is-info
          ;;        (str (k m))]
          ;;       (-> (k m)
          ;;           (str)
          ;;           (subs 0 100)))))]])
     (keys m))])

(comment
  (map-vec->keys
    [{:crux.db/id "what"
      :age 15}
     {:crux.db/id "yeah"
      :name "tom"}]))

(defn $tx-log []
  [$table {:heads ["crux.tx/tx-id"
                   "crux.tx/tx-time"
                   "crux.api/tx-ops"]
           :extra-class "is-bordered is-striped is-narrow is-fullwidth"
           :items (take 10 (-> @app-state :page/admin :tx-logs reverse))
           :row-component $tx-log-row}])

(defn $entities []
  [:div
    (map (fn [k]
           [$keys k])
         (-> @app-state :page/admin :everything))])

(defn $domains []
  [:div
    (map (fn [k]
           [$keys k])
         (->> @app-state
              :page/admin
              :everything
              (filter (fn [i]
                        (contains? i :domain/hostname)))))])
(defn $websites []
  [:div
    (map (fn [k]
           [$keys k])
         (->> @app-state
              :page/admin
              :everything
              (filter (fn [i]
                        (contains? i :website/user-id)))))])

(comment)

(defn print-code [o]
  (binding [pprint/*print-right-margin* 60
            pprint/*print-miser-width* 80]
    (pprint/with-pprint-dispatch pprint/code-dispatch
      (pprint/pprint o))))

(defn $recent-query-row [{:keys [status
                                 query-id
                                 query
                                 started-at
                                 finished-at
                                 error]}]
  [:tr
   [:td (str status)]
   [:td
    {:title (str query-id)}
    (subs query-id 0 8)]
   [:td
    {:style {:max-width 400
             :max-height 300}}
    [:pre
     (with-out-str (print-code query))]]
   [:td
    {:style {:width 110}}
    (datetime started-at)]
   [:td
    {:style {:width 110}}
    (datetime finished-at)]
   [:td (str error)]])

(defn $recent-queries []
  [:div
   [:pre
    (with-out-str (pprint/pprint (-> @app-state :page/admin :recent-queries)))]]
  [$table {:heads ["status"
                   "query-id"
                   "query"
                   "started-at"
                   "finished-at"
                   "error"]
           :extra-class "is-bordered is-striped is-narrow is-fullwidth"
           :items (-> @app-state :page/admin :recent-queries reverse)
           :row-component $recent-query-row}])

(def pages
  {:tx-log $tx-log
   :entities $entities
   :recent-queries $recent-queries
   :domains $domains})

(comment
  (def attrs (-> @app-state :page/admin :attribute-stats))
  (pprint/pprint (sort attrs)))

(defn $attribute-stats []
  [:pre
    (-> @app-state :page/admin :attribute-stats sort pprint/pprint with-out-str)])

(defn $page [child data]
  (component
    {:to-render child
     :namespace :page/admin
     :resources [[data]]}))

(def subpages
  {
   :websites {:name "Websites"
              :resource :everything
              :component $websites}
   :tx-log {:name "Transaction Log"
            :resource :tx-logs
            :component $tx-log}
   :entities {:name "Entities"
              :resource :everything
              :component $entities}
   :domains {:name "Domains"
              :resource :everything
              :component $domains}
   :recent-queries {:name "Recent Queries"
                    :resource :recent-queries
                    :component $recent-queries}
   :attribute-stats {:name "Attribute Stats"
                     :resource :attribute-stats
                     :component $attribute-stats}})

(defn $container [child]
  (let [current-page (r/atom :websites)]
    (fn []
      (let [page (get pages @current-page)
            subpage (get subpages @current-page)]
        @app-state
        [:div
         [:pre @current-page]
         [:pre (:name subpage)]
         [:div.tabs
          [:ul
            (doall
              (map (fn [[k v]]
                     ^{:key k}
                     [:li
                      {:class (when (= @current-page k) "is-active")}
                      [:a
                       {:onClick #(reset! current-page k)}
                       (:name v)]])
                   subpages))]]
         ^{:key @current-page}
         [$page (:component subpage)
                (:resource subpage)]]))))

;; (defn $admin []
;;   (component
;;     {:to-render [-$admin]
;;      :namespace :page/admin
;;      :resources [[:everything]
;;                  [:tx-logs]
;;                  [:recent-queries]]}))

(defn $admin []
  (component
    {:to-render [$container]
     :namespace :page/admin
     :resources []}));; [:everything]
                 ;; [:tx-logs]
                 ;; [:recent-queries]]}))
