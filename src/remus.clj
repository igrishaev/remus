(ns remus
  "
  Clojure wrapper around Java Rome tools.
  https://github.com/igrishaev/remus
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
  [^InputStream stream & [opt-rome]]
  (-> stream
      (rome/make-reader opt-rome)
      (rome/reader->feed)))


(defn parse-file
  [^String path & [opt-rome]]
  (let [stream (-> path io/as-file io/input-stream)]
    (parse-stream stream opt-rome)))


(defn parse-http-resp
  [http-resp & [opt-rome]]

  (let [{:keys [body]} http-resp

        content-type (http/get-content-type http-resp)
        encoding (http/get-encoding http-resp)

        opt (merge {:content-type content-type
                    :encoding encoding}
                   opt-rome)]

    (when (http/response-200? http-resp)
      (-> body
          (rome/make-reader opt)
          (rome/reader->feed)))))


(def opt-http-default
  {:as :stream
   :throw-exceptions true})


(defn parse-url
  [url & [opt-http opt-rome]]
  (let [opt (merge opt-http opt-http-default)
        resp (client/get url opt)]
    {:response resp
     :feed (parse-http-resp resp opt-rome)}))
