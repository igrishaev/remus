(ns remus.http
  (:require
   [clojure.string :as str]))


(def rss-mime-types
  #{"application/rss+xml"
    "application/rdf+xml"
    "application/atom+xml"
    "application/xml"
    "text/xml"})


(defn get-encoding
  [request]
  (some-> request
          :headers
          (get "content-encoding")
          str/lower-case))


(defn get-content-type [response]
  (some-> response
          :headers
          (get "content-type")
          (str/split #";")
          first
          str/lower-case))


(defn response-200? [response]
  (-> response :status (= 200)))


(defn response-xml? [response]
  (->> response get-content-type (contains? rss-mime-types)))
