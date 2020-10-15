(ns remus
  "Clojure wrapper around Java Rome tools.
  See https://github.com/igrishaev/remus

  https://javadoc.io/doc/com.rometools/rome/latest/index.html
  http://www.jdom.org/docs/apidocs/org/jdom2/Element.html
  http://www.jdom.org/docs/apidocs/org/jdom2/Attribute.html"

  (:require [clj-http.client :as client]
            [clojure.string :as str]
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
  (let [input          (new SyndFeedInput)
        ^SyndFeed feed (.build input reader)]
    (->clj feed)))


;;
;; Parsing
;;

(defn parse-stream
  [^InputStream stream & [opt-rome]]
  (let [{:keys [lenient ^String encoding]} opt-rome
        lenient                            (boolean lenient)]
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


(defn rome-xml-extra [els]
  (when-let [els (seq els)]
    (->xml :rome/extra
           nil
           (seq (map ->clj els)))))


(defn rome-feed-extra
  [^SyndFeed f]
  (rome-xml-extra (.getForeignMarkup f)))


(defn rome-entry-extra
  [^SyndEntry e]
  (rome-xml-extra (.getForeignMarkup e)))


(extend-type SyndEntry
  ToClojure
  (->clj [e]
    {:authors        (seq (map ->clj (.getAuthors e)))
     :categories     (seq (map ->clj (.getCategories e)))
     :contents       (seq (map ->clj (.getContents e)))
     :contributors   (seq (map ->clj (.getContributors e)))
     :enclosures     (seq (map ->clj (.getEnclosures e)))
     :description    (some-> e .getDescription ->clj)
     :author         (-> e .getAuthor not-empty)
     :link           (.getLink e)
     :published-date (.getPublishedDate e)
     :title          (.getTitle e)
     :updated-date   (.getUpdatedDate e)
     :uri            (.getUri e)
     :comments       (.getComments e)
     :extra          (rome-entry-extra e)}))


(extend-type SyndFeed
  ToClojure
  (->clj [f]
    {:authors        (seq (map ->clj (.getAuthors f)))
     :categories     (seq (map ->clj (.getCategories f)))
     :contributors   (seq (map ->clj (.getContributors f)))
     :entries        (seq (map ->clj (.getEntries f)))
     :entry-links    (seq (map ->clj (.getLinks f)))
     :image          (some-> f .getImage ->clj)
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
     :icon           (some-> f .getIcon ->clj)
     :docs           (.getDocs f)
     :generator      (.getGenerator f)
     :editor         (.getManagingEditor f)
     :webmaster      (.getWebMaster f)
     :extra          (rome-feed-extra f)}))


(defn get-xml-name [tag-ns tag-name]
  (if (str/blank? tag-ns)
    (keyword tag-name)
    (keyword tag-ns tag-name)))


(defn get-element-name
  [^Element e]
  (get-xml-name (.getNamespacePrefix e) (.getName e)))


(defn get-attribute-name
  [^Attribute a]
  (get-xml-name (.getNamespacePrefix a) (.getName a)))


(defn get-el-attrs [^Element e]
  (reduce
    (fn [result ^Attribute a]
      (assoc result (get-attribute-name a) (.getValue a)))
    nil
    (.getAttributes e)))


(extend-type Element
  ToClojure
  (->clj [e]
    (->xml
      (get-element-name e)
      (get-el-attrs e)
      (or (seq (map ->clj (.getChildren e)))
          (when-let [text (-> e .getText not-empty)]
            [text])))))


;; http://www.jdom.org/docs/apidocs/org/jdom2/Attribute.html
(extend-type Attribute
  ToClojure
  (->clj [a]
    {:name (get-attribute-name a)
     :value (.getValue a)}))
