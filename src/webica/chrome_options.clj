(ns webica.chrome-options
  "Clojure binding for Selenium class ChromeOptions"
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium.chrome
            ChromeOptions]))

(w/intern-java ChromeOptions *ns*)

