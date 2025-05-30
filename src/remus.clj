(ns remus
  "
  Clojure wrapper around Java Rome tools.
  https://github.com/igrishaev/remus

  Rome options:

  - `lenient`: a boolean flag which makes Rome to be more tolerant
    to some mistakes in XML markup;

  - `encoding`: a string representing an encoding of the feed.
    When parsing a URL, it comes from the `Content-Encoding` HTTP header.
    Possible values are listed here:
    https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html

  - `content-type`: a string meaning the MIME type of the feed, e.g. `application/rss`
    or something. When parsing a URL, it comes from the `Content-Type` header.
  "
  (:require
   [babashka.http-client :as client]
   [clojure.java.io :as io]
   [remus.http :as http]
   [remus.rome :as rome]
   [remus.util :as util])
  (:import
   java.io.InputStream))


(defmacro throw!
  ([template]
   `(throw (new RuntimeException template)))
  ([template & args]
   `(throw (new RuntimeException (format ~template ~@args)))))


;;
;; Parsing
;;

(defn parse
  "
  Parse a feed from any find of a source: a file,
  an input stream, a reader, a URL and so on.
  Internally, the source gets coerced to the input
  stream.
  "
  ([src]
   (parse src nil))

  ([src opt-rome]
   (with-open [in (io/input-stream src)]
     (-> in
         (rome/make-reader opt-rome)
         (rome/reader->feed)))))


(defn ^:deprecated parse-stream
  "
  Parse an input stream. A legacy wrapper
  on top of the `parse` function.
  "
  [^InputStream stream & [opt-rome]]
  (parse stream opt-rome))


(defn ^:deprecated parse-file
  "
  Parse a file by its text path. A legacy
  wrapper on top of the `parse` function.
  "
  [^String path & [opt-rome]]
  (parse (io/file path) opt-rome))


(defn parse-http-resp
  "
  Parse a response produced by an HTTP client. Most
  of the Rome flags come from the HTTP headers, but
  you may redefine them using the `opt-rome` map.
  "
  [http-resp & [opt-rome]]

  (let [{:keys [status
                body
                headers
                request]}
        http-resp

        {:keys [uri]}
        request

        content-type
        (http/get-content-type http-resp)

        charset
        (http/get-charset http-resp)

        opt
        (-> {:content-type content-type
             :encoding charset}
            (merge opt-rome))]

    (if (http/response-200? status)
      (if (http/response-xml? content-type)
        (parse body opt)
        (throw! "Non-XML response, status: %s, url: %s, content-type: %s"
                status uri content-type))
      (throw! "Non-200 status code, status: %s, url: %s, content-type: %s"
              status uri content-type))))


(defn parse-url
  "
  Parse feed from a URL. Return a map of the `:response`
  and `:feed` keys so you can access the response, e.g.
  to save some of the headers.

  The feed will only be parsed when the HTTP status is 200
  and the content type is XML-friendly (see possible values
  in the `remus.http` namespace).

  The `opt-http` parameter is a map of http-specific options
  which gets merged with the defaults.
  "
  [url & [opt-http opt-rome]]
  (let [opt (util/deep-merge http/opt-default opt-http)
        resp (client/get url opt)]
    {:response resp
     :feed (parse-http-resp resp opt-rome)}))
