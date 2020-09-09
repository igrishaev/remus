(def VERSION (.trim (slurp "VERSION")))

(defproject remus VERSION
  :description "Attentive RSS/Atom feed parser"

  :url "https://github.com/igrishaev/remus"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[com.rometools/rome "1.15.0"]
                 [clj-http "3.10.2"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.10.1"]]}})

