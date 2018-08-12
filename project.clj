(def VERSION (.trim (slurp "VERSION")))

(defproject remus VERSION
  :description "Attentive RSS/Atom feed parser"

  :url "https://github.com/igrishaev/remus"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[com.rometools/rome "1.11.0"]
                 [clj-http "3.6.1"]]

  :profiles {:dev {:dependencies [[org.clojure/clojure "1.9.0"]]}})
