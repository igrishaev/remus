(ns remus-test
  (:require [remus :as r]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.xml :as xml])
  (:import java.util.zip.GZIPInputStream))

(defn parse-gzip-file
  [path]
  (with-open [in (GZIPInputStream. (io/input-stream path))]
    (r/parse-stream in)))

(def youtube-extra
  {:tag   :rome/extra,
   :attrs nil,
   :content
   [{:tag :yt/videoId, :attrs nil, :content ["faoXSarGgEI"]}
    {:tag :yt/channelId, :attrs nil, :content ["UCaLlzGqiPE2QRj6sSOawJRg"]}
    {:tag   :media/group,
     :attrs nil,
     :content
     [{:tag :media/title, :attrs nil, :content ["Datomic Cloud - Datoms"]}
      {:tag     :media/content,
       :attrs
       {:url    "https://www.youtube.com/v/faoXSarGgEI?version=3",
        :type   "application/x-shockwave-flash",
        :width  "640",
        :height "390"},
       :content nil}
      {:tag     :media/thumbnail,
       :attrs
       {:url    "https://i3.ytimg.com/vi/faoXSarGgEI/hqdefault.jpg",
        :width  "480",
        :height "360"},
       :content nil}
      {:tag   :media/description,
       :attrs nil,
       :content
       ["Check out the live animated tutorial: https://docs.datomic.com/cloud/livetutorial/datoms.html\n\nYour Datomic database consists of datoms. What are Datoms?"]}
      {:tag   :media/community,
       :attrs nil,
       :content
       [{:tag     :media/starRating,
         :attrs   {:count "72", :average "5.00", :min "1", :max "5"},
         :content nil}
        {:tag :media/statistics, :attrs {:views "1997"}, :content nil}]}]}]})


(deftest parse-file
  (testing "meduza"
    (let [parsed-file (parse-gzip-file "resources/meduza.xml.gz")]
      (is (str/starts-with? (:description parsed-file) "Мы выбираем"))
      (is (str/starts-with? (-> parsed-file
                                :entries
                                first
                                :description
                                :value)
                            "Президент"))))
  (testing "youtube"
    (let [parsed-file   (parse-gzip-file "resources/youtube.xml.gz")
          extra-xml-str "<extra>\n<channelId>\nUCaLlzGqiPE2QRj6sSOawJRg\n</channelId>\n</extra>\n"]
      (is (= extra-xml-str (with-out-str
                             (xml/emit-element (:extra parsed-file)))))
      (is (= youtube-extra (-> parsed-file
                               :entries
                               first
                               :extra)))))
  (testing "empty"
    (let [parsed-file (r/parse-file "resources/empty.xml")
          nil-opts    (dissoc parsed-file :feed-type :entries)]
      (is (= "rss_2.0" (:feed-type parsed-file)))
      (is (coll? (:entries parsed-file)))
      (is (nil? (some (fn [[k v]] v) nil-opts))))))
