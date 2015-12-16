(defproject project1 "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [ring "1.4.0"]]

  :plugins [[lein-ring "0.9.7"]]

  ;;:ring {:handler project1.core/example-handler
  ;;:ring {:handler project1.core/route-handler
  ;;:ring {:handler project1.core/wrapping-handler
  :ring {:handler project1.core/full-handler
         :init    project1.core/on-init
         :port    4001
         :destroy project1.core/on-destroy})
