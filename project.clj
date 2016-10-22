(defproject startrek "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha13"]
                 [org.clojure/data.generators  "0.1.2"]
                 [org.clojure/math.numeric-tower  "0.0.4"]
                 [org.clojure/core.typed "0.3.29-SNAPSHOT"]
                 ]
  :profiles {:1.5 {:dependencies [[org.clojure/clojure "1.5.0"]]}
             :dev {:dependencies [[midje "1.9.0-alpha5"
                                   :exclusions [marick/such]]
                                  [marick/suchwow "5.2.4"]]}
             }
  :injections [(require 'clojure.core.typed)
               (clojure.core.typed/install
                 #{:load})]
  :repl-options {:timeout 10000002}
  :main startrek.core
  )
