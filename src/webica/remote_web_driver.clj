(ns webica.remote-web-driver
  "Clojure binding for Selenium class RemoteWebDriver"
  (:refer-clojure :exclude [get])
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium.remote
            RemoteWebDriver]))

(w/intern-java RemoteWebDriver *ns*
  {:clear '[quit kill]})

