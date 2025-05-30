(ns http-test
  (:require
   [remus.http :as http]
   [clojure.test :refer [is deftest]]))


(deftest test-http-charset
  (is (= "abcABC_123-987"
         (-> {:headers {"content-type" "foobar; CharSet=abcABC_123-987; dunno"}}
             (http/get-charset))))
  (is (= nil
         (-> {:headers {"content-type" "foobar; CharSet=  ; dunno"}}
             (http/get-charset)))))


(deftest test-http-content-type
  (is (= "foo-bar"
         (-> {:headers {"content-type" "Foo-Bar ; "}}
             (http/get-content-type ))))
  (is (= nil
         (-> {:headers {"content-type" nil}}
             (http/get-content-type )))))


(deftest test-http-xml?
  (is (true? (http/response-xml? "Application/Atom+Xml"))))
