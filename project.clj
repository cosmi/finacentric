(defproject
  finacentric
  "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/data.json "0.2.2"]
                 [lib-noir "0.5.2"]
                 [compojure "1.1.5"]
                 [ring-server "0.2.8"]
                 [clabango "0.5"]
                 [com.taoensso/timbre "1.6.0"]
                 [com.postspectacular/rotor "0.1.0"]
                 [com.taoensso/tower "1.5.1"]
                 [markdown-clj "0.9.19"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [korma "0.3.0-RC5"]
                 [lobos "1.0.0-beta1"]
                 [dieter "0.4.0"]
                 [http-kit "2.1.1"]
                 [hiccup "1.0.3"]
                 [clj-http "0.6.5"]
                 [enlive "1.1.1"]
                 [com.novemberain/monger "1.6.0-beta2"]
                 [log4j
                  "1.2.15"
                  :exclusions
                  [javax.mail/mail
                   javax.jms/jms
                   com.sun.jdmk/jmxtools
                   com.sun.jmx/jmxri]]]
  :native-path "native"
  :ring  {:handler finacentric.handler/war-handler,
          :init finacentric.handler/init,
          :destroy finacentric.handler/destroy}
  :profiles {:production
             {:ring
              {:open-browser? false, :stacktraces? false, :auto-reload? false}},
             :dev
             {:ring {:nrepl {:start? true :port 6060}
                     :open-browser? false}
              :dependencies [[ring-mock "0.1.3"]
                             [ring/ring-devel "1.1.8"]
                             [clojure-complete "0.2.2"]]}}
  :url "http://example.com/FIXME"
  :plugins [[lein-ring "0.8.5"]]
  :description "FIXME: write description"
  :min-lein-version "2.0.0")
