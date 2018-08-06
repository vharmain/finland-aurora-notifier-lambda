(defproject aurora-notifier "0.1.0-SNAPSHOT"
 :dependencies [[org.clojure/clojure       "1.9.0"]
                [org.clojure/clojurescript "1.10.312"]
                [io.nervous/cljs-lambda    "0.3.5"]
                [funcool/promesa "1.9.0"]]

 :plugins [[lein-npm                    "0.6.2"]
           [io.nervous/lein-cljs-lambda "0.6.6"]
           [cider/cider-nrepl "0.18.0-snapshot"]]

 :npm {:dependencies [[serverless-cljs-plugin "0.1.2"
                       request-promise "4.2.2"
                       request "2.87.0"
                       cheerio "1.0.0-rc.2"]]
       :devDependencies [[aws-sdk "2.286.2"]]}

 :profiles {:dev {:dependencies [[cider/piggieback "0.3.8"]]
                  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}}

 :cljs-lambda {:compiler
               {:inputs  ["src"]
                :options {:output-to     "target/aurora-notifier/aurora_notifier.js"
                          :output-dir    "target/aurora-notifier"
                          :target        :nodejs
                          :language-in   :ecmascript5
                          :optimizations :simple}}})
