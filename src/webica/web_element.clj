(ns webica.web-element
  "Clojure binding for Selenium class WebElement"
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium
            WebElement]))

(w/intern-java WebElement *ns*)

