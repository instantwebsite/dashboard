(ns state
  (:require
    [reagent.core :as r]
    [ls :as ls]))

(defonce app-state (r/atom {:tokens {:access nil
                                     :api nil}
                            :notifications '()
                            :debug? (or (ls/get :debug?) false)
                            :username nil
                            :websites #{}
                            :domains #{}
                            :domain nil
                            :new-domain nil
                            :current-page "/"}))
