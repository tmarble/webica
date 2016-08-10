(ns webica.chrome-driver-service
  "Clojure binding for Selenium class ChromeDriverService"
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium.chrome
            ChromeDriverService]))

(w/intern-java ChromeDriverService *ns*)

