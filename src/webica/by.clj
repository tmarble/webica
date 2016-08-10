(ns webica.by
  "Clojure binding for Selenium class By"
  (:refer-clojure :exclude [name])
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium
            By]))

(w/intern-java By *ns*
  {:coercions
   [["org.openqa.selenium.By$ByClassName" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByCssSelector" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ById" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByLinkText" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByName" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByPartialLinkText" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByTagName" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByXPath" "org.openqa.selenium.By" identity]]})

