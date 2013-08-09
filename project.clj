(defproject finacentric "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.cosmi.causeway "0.2.2-SNAPSHOT"]
                 [ring "1.2.0"]
                 [ring-http-basic-auth "0.0.2"]
                 ;;lobos + extra deps
                 [lobos "1.0.0-beta1"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 ;;korma + extra deps
                 [korma "0.3.0-RC5"]
                 [org.clojure/java.jdbc "0.3.0-alpha4"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]]
  :plugins [[lein-ring "0.8.6"]
            [lein-pprint "1.1.1"]]
  :ring  {:handler finacentric.handler/main-handler,
          :init finacentric.handler/init,
          :destroy finacentric.handler/destroy
          }

  :main finacentric.devtools

  :profiles {
             :production {:jvm-opts ["-Dbootconfig=bootconfig/prod.clj"]
                          :ring
                          {:open-browser? false, :stacktraces? false, :auto-reload? false}}
             :dev {:jvm-opts ["-Dbootconfig=bootconfig/dev.clj"]
                   :ring {:nrepl {:start? true :port 6060}
                          :open-browser? false}}
             })
