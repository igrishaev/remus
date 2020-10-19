(ns remus
  "
  Clojure wrapper around Java Rome tools.
  https://github.com/igrishaev/remus

  Rome options:

  - `lenient`: a boolean flag which makes Rome to be more loyal
    to some mistakes in XML markup;

  - `encoding`: a string which represents the encoding of the feed.
    When parsing a URL, it comes from the `Content-Encoding` HTTP header.
    Possible values are listed here:
    https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html

  - `content-type`: a string meaning the MIME type of the feed, e.g. `application/rss`
    or something. When parsing a URL, it comes from the `Content-Type` header.
  "

  (:require
   [remus.rome :as rome]
   [remus.http :as http]

   [clojure.java.io :as io]

   [clj-http.client :as client])

  (:import
   java.io.InputStream))


;;
;; Parsing
;;

(defn parse-stream
  "
  An utility function used as a bottleneck
  for parsing a file, URL, etc.
  "
  [^InputStream stream & [opt-rome]]
  (-> stream
      (rome/make-reader opt-rome)
      (rome/reader->feed)))


(defn parse-file
  "
  Parse a feed from a file. Path is a string
  referencing a file on disk.
  "
  [^String path & [opt-rome]]
  (let [stream (-> path io/as-file io/input-stream)]
    (parse-stream stream opt-rome)))


(defn parse-http-resp
  "
  Parse an HTTP response produced by the `clj-http` client
  (`aleph.http` might also work). Most of the Rome flags
  come from the HTTP headers, but you may redefine them
  in the `opt-rome` map.
  "
  [http-resp & [opt-rome]]

  (let [{:keys [body]} http-resp

        content-type (http/get-content-type http-resp)
        encoding (http/get-encoding http-resp)

        opt (merge {:content-type content-type
                    :encoding encoding}
                   opt-rome)]

    (when (and (http/response-200? http-resp)
               (http/response-xml? http-resp))
      (-> body
          (rome/make-reader opt)
          (rome/reader->feed)))))


(defn parse-url
  "
  Parse feed from a URL. Returns a map of the `:response`
  and `:feed` keys so you can access the response, e.g.
  to save some of the headers.

  The feed will only be parsed when the HTTP status is 200
  and the content type is XML-friendly (see the possible values
  in the `remus.http` namespace).

  The `opt-http` parameter is a map of `clj-http/aleph` options
  which gets merged with the defaults. The `:as` parameters always
  becomes `:stream` to reuse the `parse-stream` function.
  "
  [url & [opt-http opt-rome]]
  (let [opt (merge opt-http http/opt-default)
        resp (client/get url opt)]
    {:response resp
     :feed (parse-http-resp resp opt-rome)}))
