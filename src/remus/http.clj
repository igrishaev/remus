(ns remus.http
  (:require
   [clojure.string :as str]))


(def opt-default
  {:as :stream
   :throw false})


(def RSS_MIME_TYPES
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


(defn response-200? [status]
  (= status 200))


(defn response-xml? [content-type]
  (contains? RSS_MIME_TYPES content-type))
