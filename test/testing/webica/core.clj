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
            [webica.web-element :as element]))

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
    (is (= expected-title
          (let [_ (firefox/start-firefox)
                title (cheese)]
            (browser/quit)
            title)))
    (is (= expected-title
          (let [_ (chrome/start-chrome)
                title (cheese)]
            (browser/quit)
            title)))))
