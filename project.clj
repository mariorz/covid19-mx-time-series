(defproject covid19-mx-time-series "0.1.0-SNAPSHOT"
  :description "Project used to generate time-series data for the covid19 epidemic in Mexico"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[cider/cider-nrepl "0.24.0"]]
  :middleware [cider-nrepl.plugin/middleware]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.csv "0.1.4"]
                 [clj-http "3.10.0"]
                 [nrepl "0.7.0-beta1"]]
  :main ^:skip-aot covid19-mx-time-series.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
