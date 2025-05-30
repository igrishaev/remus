(ns remus.http
  (:require
   [clojure.string :as str]))


(def opt-default
  {:as :stream
   :throw false
   :headers {"accept-encoding" ["gzip" "deflate"]}})


(def RSS_MIME_TYPES
  #{"application/rss+xml"
    "application/rdf+xml"
    "application/atom+xml"
    "application/xml"
    "text/xml"})


(defn get-charset
  [request]
  (when-let [content-type
             (some-> request
                     :headers
                     (get "content-type"))]
    (-> #"(?i)charset=([a-zA-Z0-9-_]+)"
        (re-find content-type)
        (second))))


(defn get-content-type [response]
  (some-> response
          :headers
          (get "content-type")
          (str/split #";")
          first
          str/trim
          str/lower-case))


(defn response-200? [status]
  (= status 200))


(defn response-xml? [content-type]
  (contains? RSS_MIME_TYPES
             (str/lower-case content-type)))
