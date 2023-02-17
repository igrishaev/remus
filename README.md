# Remus

[rome-site]: https://rometools.github.io/rome/

[clj-http]: https://github.com/dakrone/clj-http

An attentive RSS and Atom feed parser for Clojure. It's built on top of
well-known and powerful [ROME Tools][rome-site] Java library. Remus deals with
weird encoding and non-standard XML tags. The library fetches as much
information from a feed as possible.

![](art/romulus-remus.jpg)

# Table of Contents

<!-- toc -->

- [Benefits](#benefits)
- [Installation](#installation)
- [Usage](#usage)
  * [Parsing a URL](#parsing-a-url)
  * [Parsing a file](#parsing-a-file)
  * [Parsing an input stream](#parsing-an-input-stream)
- [HTTP tweaks](#http-tweaks)
  * [Errors and exceptions](#errors-and-exceptions)
  * [Saving headers](#saving-headers)
- [Non-standard tags](#non-standard-tags)
- [Encoding issues](#encoding-issues)
- [License](#license)

<!-- tocstop -->

## Benefits

- Gets all the known fields from a feed and turns them into plain Clojure data
  structures;
- relies on up-to-date ROME release;
- uses the power of [clj-http][clj-http] client instead of deprecated ROME
  Fetcher;
- preserves all the non-standard XML tags for further processing (see example
  below).

## Installation

Leiningen/Boot:

```clojure
[remus "0.2.4"]
```

Clojure CLI/deps.edn

```clojure
remus/remus {:mvn/version "0.2.4"}
```

## Usage

The library provides a one-word top namespace `remus` so it's easier to
remember.

```clojure
(ns your.project
  (:require [remus :refer [parse-url parse-file]]))
```

or:

```clojure
(require '[remus :refer [parse-url parse-file]])
```


### Parsing a URL

Let's parse [Planet Clojure](http://planet.clojure.in/):

```clojure
(def result (parse-url "http://planet.clojure.in/atom.xml"))
```

The variable `result` is a map of two keys: `:response` and `:feed`. These are
an HTTP response and a parsed feed. Below, there is a truncated version of a
feed:

```clojure
(def feed (:feed result))

(println feed)

;;;;
;; just a small subset
;;;;

{:description nil,
 :feed-type "atom_1.0"
 :entries
 [{:description nil
   :updated-date #inst "2018-08-13T10:00:00.000-00:00"
   :extra {:tag :extra, :attrs nil, :content ()}
   :title
   "PurelyFunctional.tv Newsletter 287: DataScript, GraphQL, CRDTs"
   :author "Eric Normand"
   :link
   "https://purelyfunctional.tv/issues/purelyfunctional-tv-newsletter-287-datascript-graphql-crdts/"
   :uri "https://purelyfunctional.tv/?p=28660"
   :contents
   ({:type "html"
     :mode nil
     :value
     "<div class=\" reset\">\n<p><em>Issue 287 August 13, 2018 <a href=\"https://purelyfunctional.tv/newsletter-archives/\">Archives</a> <a href=\"https://purelyfunctional.tv/newsletter/\" title=\"Thanks, Jeff!\">Subscribe</a></em></p>\n<p>Hi Clojurationists,</p>\n<p>I've just been digging <a href=\"https://twitter.com/puredanger/status/1028103654241443840\" title=\"\">this lovely tweet from Alex Miller</a>.</p>\n<p>Rock on!<br /><a href=\"http://twitter.com/ericnormand\">Eric Normand</a> &lt;<a href=\"mailto: ... "}),
 :published-date #inst "2018-08-13T11:59:11.000-00:00"
 :entry-links
 ({:rel "alternate"
   :href "http://planet.clojure.in/"
   :length 0}
  {:rel "self"
   :href "http://planet.clojure.in/atom.xml",
   :length 0})
 :title "Planet Clojure"
 :language nil
 :link "http://planet.clojure.in/"
 :uri "http://planet.clojure.in/atom.xml"
 :authors ()}
```

</details>

As for HTTP response, it's the same data structure that
`clj-http.client/response` function returns. You might need that data to save
some of the HTTP headers for further requests (see below).

### Parsing a file

```clojure
(def feed (parse-file "/path/to/some/atom.xml"))
```

This function just returns a parsed feed.

### Parsing an input stream

Just in case you're getting a feed from a stream, here is a function for that:

```clojure
(def feed (parse-stream (clojure.java.io/input-stream some-source)))
```

Like `parse-file`, it returns a parsed feed as a data structure.

## HTTP tweaks

Since `Remus` relies on [clj-http][clj-http] library for HTTP communication, you
are welcome to use all its features. For example, to control redirects, security
validation, authentication, etc. When calling `parse-url`, pass an optional map
with HTTP parameters:

```clojure
;; Do not check an untrusted SSL certificate.
(parse-url "http://planet.clojure.in/atom.xml"
           {:insecure? true})


;; Parse a user/pass protected HTTP resource.
(parse-url "http://planet.clojure.in/atom.xml"
           {:basic-auth ["username" "password"]})


;; Pretending being a browser. Some sites protect access by "User-Agent" header.
(parse-url "http://planet.clojure.in/atom.xml"
           {:headers {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac...."}})
```

Remus overrides **just one option** which is `:as`. No matter what you put into
it, the value becomes `:stream`. We need a streamed HTTP response because ROME
relies on an input stream.

### Errors and exceptions

It's up to you how to deal with non-200 HTTP responses. Even if you pass
`{:throw-exceptions false}`, the feed only be parsed when the status code is
200.

```clojure
(let [result (parse-url "http://example.com/non-existing-url"
                               {:throw-exceptions false})
             {:keys [response feed]} result]
         (when-not feed
           (process-non-200 response)))
```

Or just skip the `:throw-exceptions` flag and wrap everything into the standard
`try/catch` form:

```clojure
(try
  (parse-url "http://non-existing-url")
  (catch clojure.lang.ExceptionInfo e
    (let [response (ex-data e)
          {:keys [status headers]} response]
      (println status headers)
      ;; do anything you want
      )))
```

[clj-http-ex]:https://github.com/dakrone/clj-http#exceptions

[slingshot]: https://github.com/scgilardi/slingshot

Alternately, you may use the [Slingshot][slingshot] approach to catch
HTTP-thrown exceptions as the [official manual][clj-http-ex] describes.

### Saving headers

[cond-get]: https://fishbowl.pastiche.org/2002/10/21/http_conditional_get_for_rss_hackers

When parsing a URL, a good option would be to pass the `If-None-Match` and
`If-Modified-Since` headers with the values from the `Etag` and `Last-Modified`
ones from the previous response. This trick is know as [conditional
GET][cond-get]. It might prevent server from sending the data you've already
received before:

```clojure
;; returns the whole feed
(def result (parse-url "http://planet.lisp.org/rss20.xml"))

;; split the result
(def feed (:feed result))
(def response (:response result))

;; ensure we got the data
(:length response)
48082

;; save the headers
(def etag (-> response :headers :etag))
;; "5b71766f-2f597"

(def last-modified (-> response :headers :last-modified))
;; Mon, 19 Oct 2020 12:15:27 GMT

;;;;
;; Now, try to fetch data passing conditionals headers:
;;;;

(def result-new
  (parse-url "http://planet.lisp.org/rss20.xml"
             {:headers {"If-None-Match" etag
                        "If-Modified-Since" last-modified}}))

(-> result-new :response :status)
304

(-> result-new :response :length)
0

(-> result-new :feed)
nil
```

Since the server returned non-200 but positive status code (304 in our case), we
don't parse the response at all. So the `:feed` field in the `result-new`
variable will be `nil`.

## Non-standard tags

[youtube-rss]: https://www.youtube.com/feeds/videos.xml?channel_id=UCaLlzGqiPE2QRj6sSOawJRg

Sometimes, a feed ships additional data with non-standard tags. A good example
might be a typical [YouTube feed][youtube-rss]. Let's examine one of its
entries:

```xml
<entry>
  <id>yt:video:TbthtdBw93w</id>
  <yt:videoId>TbthtdBw93w</yt:videoId>
  <yt:channelId>UCaLlzGqiPE2QRj6sSOawJRg</yt:channelId>
  <title>Datomic Ions in Seven Minutes</title>
  <link rel="alternate" href="https://www.youtube.com/watch?v=TbthtdBw93w"/>
  <author>
    <name>ClojureTV</name>
    <uri>
      https://www.youtube.com/channel/UCaLlzGqiPE2QRj6sSOawJRg
    </uri>
  </author>
  <published>2018-07-03T21:16:16+00:00</published>
  <updated>2018-08-09T16:29:51+00:00</updated>
  <media:group>
    <media:title>Datomic Ions in Seven Minutes</media:title>
    <media:content url="https://www.youtube.com/v/TbthtdBw93w?version=3" type="application/x-shockwave-flash" width="640" height="390"/>
    <media:thumbnail url="https://i1.ytimg.com/vi/TbthtdBw93w/hqdefault.jpg" width="480" height="360"/>
    <media:description>
      Stuart Halloway introduces Ions for Datomic Cloud on AWS.
    </media:description>
    <media:community>
      <media:starRating count="67" average="5.00" min="1" max="5"/>
      <media:statistics views="1977"/>
    </media:community>
  </media:group>
</entry>
```

In addition to the standard fields, the feed carries information about the video
ID, channel ID and statistics: views count, the number of times the video was
starred and its average rating. You would probably want to use that data.

Alternately, if you parse a geo-related feed, you'll get lat/lot coordinates,
location names, tracks, etc.

Other RSS parsers either drop this data or require you to write a custom
extension. `Remus` provides all the non-standard tags as a parsed XML
structure. It puts that data into an `:extra` field for each entry and on the
top level of a feed. This is how you can reach it:

```clojure
(def result (parse-url "https://www.youtube.com/feeds/videos.xml?channel_id=UCaLlzGqiPE2QRj6sSOawJRg"))

(def feed (:feed result))

;;;;
;; Get entry-specific custom data
;;;;

;; Extra data from the first entry:
(-> feed :entries first :extra)

{:tag :rome/extra
 :attrs nil
 :content
 ({:tag :yt/videoId :attrs nil :content ["faoXSarGgEI"]}
  {:tag :yt/channelId :attrs nil :content ["UCaLlzGqiPE2QRj6sSOawJRg"]}
  {:tag :media/group
   :attrs nil
   :content
   ({:tag :media/title :attrs nil :content ["Datomic Cloud - Datoms"]}
    {:tag :media/content
     :attrs
     {:url "https://www.youtube.com/v/faoXSarGgEI?version=3"
      :type "application/x-shockwave-flash"
      :width "640"
      :height "390"}
     :content nil}
    {:tag :media/thumbnail
     :attrs
     {:url "https://i3.ytimg.com/vi/faoXSarGgEI/hqdefault.jpg"
      :width "480"
      :height "360"}
     :content nil}
    {:tag :media/description
     :attrs nil
     :content
     ["Check out the live animated tutorial: https://docs.datomic.com/cloud/livetutorial/datoms.html\n\nYour Datomic database consists of datoms. What are Datoms?"]}
    {:tag :media/community
     :attrs nil
     :content
     ({:tag :media/starRating
       :attrs {:count "72" :average "5.00" :min "1" :max "5"}
       :content nil}
      {:tag :media/statistics :attrs {:views "2014"} :content nil})})})}


;;;;
;; Get feed-specific extra:
;;;;

(-> feed :extra)

{:tag :rome/extra
 :attrs nil
 :content
 ({:tag :yt/channelId :attrs nil :content ["UCaLlzGqiPE2QRj6sSOawJRg"]})}
```

The `:extra` fields follow the standard XML-friendly structure so they can be
processed with any XML-related technics like walking, zippers, etc.

## Encoding issues

All the `parse-<something>` functions mentioned above take additional
ROME-related options. Use them to solve XML-decoding issues when dealing with
weird or non-set HTTP headers. ROME's got a solid algorithm to guess encoding,
but sometimes it might need your help.

At the moment, Remus supports `:lenient`, `:encoding` and `content-type` options
with has the following meaning:

- `lenient`: a boolean flag which makes Rome to be more loyal to some mistakes
  in XML markup;

- `encoding`: a string which represents the encoding of the feed.  When parsing
  a URL, it comes from the `Content-Encoding` HTTP header.  Possible values are
  listed here: https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html

- `content-type`: a string meaning the MIME type of the feed,
  e.g. `application/rss` or something. When parsing a URL, it comes from the
  `Content-Type` header.

Dealing with Windows encoding and unset `Content-type` or `Content-Encoding`
headers:

```clojure
(parse-url "https://some/rss.xml" nil {:lenient true :encoding "cp1251"})
```

The same options work for parsing a file or a stream:

```clojure
(parse-file "https://another/atom.xml" {:lenient true :encoding "cp1251"})

(parse-stream in-source {:lenient true :encoding "cp1251"})
```

## License

Copyright © 2020 Ivan Grishaev

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
