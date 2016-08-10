(ns webica.remote-web-element
  "Clojure binding for Selenium class RemoteWebElement"
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium.remote
            RemoteWebElement]))

(w/intern-java RemoteWebElement *ns*)

