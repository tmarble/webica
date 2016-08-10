(ns webica.chrome-driver
  "Clojure binding for Selenium class ChromeDriver"
  (:refer-clojure :exclude [get])
  (:require [clojure.core :as clj]
            [webica.core :as w]
            [me.raynes.fs :as fs])
  (:import [org.openqa.selenium.chrome
            ChromeDriver]))

(w/intern-java ChromeDriver *ns*
  {:clear '[quit kill]})

(defn start-chrome [&[chromedriver]]
  (let [driver-prop "webdriver.chrome.driver"
        default-exe (System/getProperty driver-prop)
        chromedriver-system "/usr/lib/chromium/chromedriver"
        chromedriver-local "/usr/local/bin/chromedriver"
        chromedriver (or chromedriver
                     (if (and (not (empty? default-exe))
                           (fs/exists? default-exe))
                       default-exe)
                     (if (fs/exists? chromedriver-system)
                       chromedriver-system)
                     chromedriver-local)]
    (if-not (fs/exists? chromedriver)
      (throw
        (RuntimeException.
          (str "ERROR: chromedriver executable not found:" chromedriver))))
    (System/setProperty driver-prop chromedriver)
    (instance)))