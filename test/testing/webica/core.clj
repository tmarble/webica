;; Copyright (c) 2016 Tom Marble
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns testing.webica.core
  (:require [clojure.string :as string]
            [clojure.test :refer [deftest testing is]]
            [webica.core :as w :refer [sleep]]
            [webica.by :as by]
            [webica.chrome-driver :as chrome]
            [webica.firefox-driver :as firefox]
            [webica.remote-web-driver :as browser]
            [webica.web-driver :as driver]
            [webica.web-driver-wait :as wait]
            [webica.web-element :as element]
            [webica.expected-conditions :as ec]))

;; inspired by
;; http://docs.seleniumhq.org/docs/03_webdriver.jsp#introducing-the-selenium-webdriver-api-by-example"
(def expected-title "Cheese! - Google Search")

(defn cheese []
  (browser/get "http://www.google.com")
  (let [q (browser/find-element (by/name "q"))]
    (element/send-keys q "Cheese!")
    (element/submit q)
    (wait/until (wait/instance 10)
                (wait/condition
                  (fn [driver]
                    (string/starts-with?
                      (string/lower-case (driver/get-title driver))
                      "cheese!"))))
    (w/sleep 2)
    (browser/get-title)))

(deftest testing-webica-core
  (testing "testing-webica-core"
    #_
    (is (= expected-title
           (let [_     (firefox/start-firefox)
                 title (cheese)]
             (browser/quit)
             title)))
    (is (= expected-title
           (let [_     (chrome/start-chrome)
                 title (cheese)]
             (browser/quit)
             title)))))

(def search-term "Clojure Conj 2017")

;; Note: the actual url for the above search term is like:
;; "https://www.google.com/search?site=&source=hp&q=Clojure+Conj+2017&oq=&gs_l="
(def partial-url (apply str (interpose "+" (clojure.string/split search-term #"\s+"))))

(defn lmgtfy [search]
  (browser/get "http://www.google.com")
  (let [wdriver (wait/init-web-driver-wait (driver/get-instance))
        q       (browser/find-element (by/name "q"))]
    (element/send-keys q search)
    (element/submit q)
    (wait/until wdriver (ec/title-contains search))
    (driver/get-current-url)))

(deftest testing-webica-core-with-expected-conditions
  (testing "testing-webica-core-with-expected-conditions"
    (let [_          (firefox/start-firefox)
          actual-url (lmgtfy search-term)]
      (browser/quit)
      (is (string/includes? actual-url partial-url)))

    (let [chrome-binary (chrome/get-chromedriver-binary "/usr/lib/chromium/chromedriver")
          _             (chrome/start-chrome chrome-binary "headless") ;; Note: needed chromedriver version 2.30.x+ which support headless
          actual-url    (lmgtfy search-term)]
      (browser/quit)
      (is (string/includes? actual-url partial-url)))))
