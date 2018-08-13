(ns remus
  "Clojure wrapper around Java Rome tools.
  See https://github.com/igrishaev/remus"

  (:require [clj-http.client :as client]

            [clojure.java.io :as io]
            [clojure.xml :as xml])

  (:import java.net.URL

           [java.io File InputStream ByteArrayInputStream]

           [org.jdom2 Element Attribute]

           [com.rometools.rome.feed.synd
            SyndFeed
            SyndEntry
            SyndImage
            SyndCategory
            SyndPerson
            SyndLink
            SyndContent
            SyndEnclosure]

           [com.rometools.rome.io
            SyndFeedInput
            XmlReader]))


;;
;; Utilities
;;


(defprotocol ToClojure
  (->clj [obj]))


(defn- reader->feed
  [^XmlReader reader]
  (let [input (new SyndFeedInput)
        ^SyndFeed feed (.build input reader)]
    (->clj feed)))


;;
;; Parsing
;;


(defn parse-stream
  [^InputStream stream & [opt-rome]]
  (let [{:keys [lenient encoding]} opt-rome
        lenient (boolean lenient)]
    (reader->feed
     (XmlReader. ^InputStream stream lenient encoding))))


(defn parse-file
  [^String path & [opt-rome]]
  (let [stream (io/input-stream (io/as-file path))]
    (parse-stream stream opt-rome)))


(defn- parse-http-resp
  [http-resp & [opt-rome]]
  (let [{:keys [status body headers]} http-resp
        {:keys [^String content-type]} headers

        {:keys [lenient ^String encoding]} opt-rome
        lenient (boolean lenient)]

    (when (= status 200)
      (reader->feed
       (XmlReader. ^InputStream body content-type lenient encoding)))))


(def opt-http-default
  {:as :stream
   :throw-exceptions true})


(defn parse-url
  [url & [opt-http opt-rome]]
  (let [opt (merge opt-http opt-http-default)
        resp (client/get url opt)]
    {:response resp
     :feed (parse-http-resp resp opt-rome)}))


;;
;; To Clojure
;;


(extend-type SyndContent
  ToClojure
  (->clj [c]
    {:type  (.getType c)
     :mode  (.getMode c)
     :value (.getValue c)}))


(extend-type SyndEnclosure
  ToClojure
  (->clj [e]
    {:url    (.getUrl e)
     :length (.getLength e)
     :type   (.getType e)}))


(extend-type SyndLink
  ToClojure
  (->clj [l]
    {:rel       (.getRel l)
     :type      (.getType l)
     :href      (.getHref l)
     :title     (.getTitle l)
     :href-lang (.getHreflang l)
     :length    (.getLength l)}))


(extend-type SyndPerson
  ToClojure
  (->clj [p]
    {:name  (.getName p)
     :uri   (.getUri p)
     :email (.getEmail p)}))


(extend-type SyndCategory
  ToClojure
  (->clj [c]
    {:name         (.getName c)
     :taxonomy-url (.getTaxonomyUri c)}))


(extend-type SyndImage
  ToClojure
  (->clj [i]
    {:title       (.getTitle i)
     :url         (.getUrl i)
     :width       (.getWidth i)
     :height      (.getHeight i)
     :link        (.getLink i)
     :description (.getDescription i)}))


(def ->xml (partial struct xml/element))


(defn rome-extra
  [obj]
  (->xml :extra
         nil
         (map ->clj (.getForeignMarkup obj))))


(extend-type SyndEntry
  ToClojure
  (->clj [e]
    {:authors        (map ->clj (seq (.getAuthors e)))
     :categories     (map ->clj (seq (.getCategories e)))
     :contents       (map ->clj (seq (.getContents e)))
     :contributors   (map ->clj (seq (.getContributors e)))
     :enclosures     (map ->clj (seq (.getEnclosures e)))
     :description    (when-let [d (.getDescription e)]
                       (->clj d))
     :author         (.getAuthor e)
     :link           (.getLink e)
     :published-date (.getPublishedDate e)
     :title          (.getTitle e)
     :updated-date   (.getUpdatedDate e)
     :uri            (.getUri e)
     :comments       (.getComments e)

     :extra          (rome-extra e)}))


(extend-type SyndFeed
  ToClojure
  (->clj [f]
    {:authors        (map ->clj (seq (.getAuthors f)))
     :categories     (map ->clj (seq (.getCategories f)))
     :contributors   (map ->clj (seq (.getContributors f)))
     :entries        (map ->clj (seq (.getEntries f)))
     :entry-links    (map ->clj (seq (.getLinks f)))
     :image          (when-let [i (.getImage f)]
                       (->clj i))
     :author         (.getAuthor f)
     :copyright      (.getCopyright f)
     :description    (.getDescription f)
     :encoding       (.getEncoding f)
     :feed-type      (.getFeedType f)
     :language       (.getLanguage f)
     :link           (.getLink f)
     :published-date (.getPublishedDate f)
     :title          (.getTitle f)
     :uri            (.getUri f)
     :icon           (when-let [i (.getIcon f)]
                       (->clj i))
     :docs           (.getDocs f)
     :generator      (.getGenerator f)
     :editor         (.getManagingEditor f)
     :webmaster      (.getWebMaster f)

     :extra          (rome-extra f)}))


(defn xml-full-name
  [obj]
  (keyword (.getNamespacePrefix obj)
           (.getName obj)))


;; http://www.jdom.org/docs/apidocs/org/jdom2/Element.html
(extend-type Element
  ToClojure
  (->clj [e]

    (->xml
     ;; name
     (xml-full-name e)

     ;; attrs
     (not-empty
      (into {}
            (map (juxt :name :value)
                 (map ->clj (.getAttributes e)))))

     ;; children
     (or (not-empty (map ->clj (.getChildren e)))
         (not-empty (.getText e))))))


;; http://www.jdom.org/docs/apidocs/org/jdom2/Attribute.html
(extend-type Attribute
  ToClojure
  (->clj [a]
    {:name (xml-full-name a)
     :value (.getValue a)}))
