(ns config)

(def api-domain {:dev "http://localhost:8080/api/"
                 :production "https://api.instantwebsite.app/"})

(def website-preview-domains
  {:dev "http://localhost:8888/"
   :production "https://websites.instantwebsite.app/"})

(defn get-host []
  (-> js/window .-location .-hostname))

(def current-env (if (= (get-host)
                        "localhost")
                   :dev
                   :production))

(def config {:api (get api-domain current-env)
             :website-preview (get website-preview-domains current-env)})
