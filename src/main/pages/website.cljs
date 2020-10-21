(ns pages.website
  (:require
    [clojure.pprint :refer [pprint]]
    [page-component :refer [component]]
    [config :refer [config]]
    [components.text-input :refer [$text-input]]
    [state :refer [app-state]]
    [components.table :refer [$table $table-row]]
    [tick.alpha.api :as t]
    [tick.locale-en-us :as l]))

(defn format-time [ti]
  (t/format
    (tick.format/formatter "YYYY-MM-dd - HH:mm:ss")
    (t/date-time ti)))

(comment
  (t/date-time #inst "2020-10-02T21:05:06.822-00:00")
  (format-time #inst "2020-10-02T21:05:06.822-00:00"))

(defn construct-visit-url [website-id content-hash]
  (str (:website-preview config)
       website-id
       "/"
       content-hash))

(defn $history-row [history]
  [$table-row {:id (:crux.db/content-hash history)
               :datas [(:index history)
                       [:div
                        {:title (:crux.db/content-hash history)}
                        (subs
                          (:crux.db/content-hash history)
                          0
                          8)]
                       (format-time (:crux.db/valid-time history))
                       (when-not (= (:index history) 0)
                         [:a.button
                          {:href (construct-visit-url (:website-id history)
                                                      (:crux.db/content-hash history))
                           :target "_blank"}
                          "View"])]}])

(defn $name []
  [:div
   [$text-input {:value (-> @app-state
                            :page/website
                            :website
                            :website/name)}]])

(defn -$website [{:keys [id]}]
  [:div.box
    [:h1.title
     "Edit website"]
    [$name]
    [:h1.subtitle (-> @app-state :page/website :website :website/name)]
    [:div]
    [:h3.subtitle "History"]
    [$table {:items (->> @app-state
                         :page/website
                         :website-history
                         (map (fn [history]
                                (assoc history :website-id id)))
                         (reverse)
                         (map-indexed (fn [index history]
                                        (assoc history :index index)))
                         (reverse))
             :heads ["Version" "Content-Hash" "Created At" ""]
             :row-component $history-row}]])

(defn $website [{:keys [id]
                 :as opts}]
  (component
    {:to-render [-$website opts]
     :namespace :page/website
     :resources [[:website id]
                 [:website-history id]]}))
