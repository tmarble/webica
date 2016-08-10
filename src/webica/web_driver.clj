(ns webica.web-driver
  "Clojure binding for Selenium class WebDriver"
  (:refer-clojure :exclude [get])
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium
            WebDriver]))

(w/intern-java WebDriver *ns*)

