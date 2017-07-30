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


(defn ^:private init-chrome-options
  "The default ChromeOptions with the web-driver initialization.
  e.g.
  (init-chrome-options \"headless\"
                       \"--disable-gpu\"
                       \"--window-size=1920,1080\")"
  [& args]
  (let [chrome-options (org.openqa.selenium.chrome.ChromeOptions.)
        flags (vec (first args))]
    ;; Call ChromeOptions.addArguments()
    (.addArguments chrome-options flags)
    chrome-options))

(defn start-chrome [chromedriver & args]
  "The default chrome options with the web-driver initialization.

  Example Usage:

  (a) Create chrome-driver without chrome-options :

  (start-chrome \"/usr/lib/chromium/chromedriver\")

  (b) Create chrome-driver with chrome-options :

  (start-chrome \"/usr/lib/chromium/chromedriver\"
                ;; Arguments for init-chrome-options above.
                \"headless\"                 ;; Run headless (omit to run non-headless)
                \"--disable-gpu\"            ;; mandatory argument for Chrome/Chromium 59.0
                \"--window-size=1920,1080\") ;; optional but good to have
  For complete options please see https://goo.gl/DcUcrj"
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
    (if args
      (instance (init-chrome-options args))
      (instance))))