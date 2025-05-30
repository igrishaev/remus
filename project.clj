(defproject remus "0.2.5-SNAPSHOT"
  :description "Attentive RSS/Atom feed parser"

  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}

  :release-tasks
  [["vcs" "assert-committed"]
   ["test"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["vcs" "commit"]
   ["vcs" "tag" "--no-sign"]
   ["deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "push"]]

  :url
  "https://github.com/igrishaev/remus"

  :license
  {:name "The Unlicense"
   :url "https://unlicense.org/"}

  :managed-dependencies
  [[org.clojure/clojure "1.10.1"]
   [com.rometools/rome "1.18.0"]
   [org.babashka/http-client "0.4.22"]
   [log4j/log4j "1.2.17"]]

  :dependencies
  [[org.clojure/clojure :scope "provided"]
   [com.rometools/rome]
   [org.babashka/http-client]]

  :profiles
  {:dev {:dependencies [[org.clojure/clojure]
                        [log4j/log4j]]
         :global-vars  {*warn-on-reflection* true
                        *assert*             true}}})
