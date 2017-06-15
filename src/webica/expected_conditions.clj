(ns webica.expected-conditions
  "Clojure binding for Selenium class ExpectedConditions"
  (:refer-clojure :exclude [not or and])
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium.support.ui
            ExpectedConditions]))

(w/intern-java ExpectedConditions *ns*)

