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

(def driver-prop "webdriver.chrome.driver")

(defn ^:private common-chrome-binary
  "Get chromedriver binary from a commonly installed location."
  []
  (let [default-exe         (System/getProperty driver-prop)
        chromedriver-system "/usr/lib/chromium/chromedriver"
        chromedriver-local  "/usr/local/bin/chromedriver"
        chromedriver        (or (if (and (not (empty? default-exe))
                                         (fs/exists? default-exe))
                                  default-exe)
                                (if (fs/exists? chromedriver-system)
                                  chromedriver-system)
                                chromedriver-local)]
    chromedriver))

(defn ^:private validate-chromedriver
  [driver-binary]
  (if-not (fs/exists? driver-binary)
    (throw
     (RuntimeException.
      (str "ERROR: chromedriver executable not found:" driver-binary))))
  driver-binary)

;; Public apis

(defn get-chromedriver-binary
  "Get current chromedriver binary from a given path or from default location."
  ([]
   (get-chromedriver-binary (common-chrome-binary)))
  ([driver-binary]
   (validate-chromedriver driver-binary)))

(defn start-chrome
  "The default chrome options with the web-driver initialization.

  Example Usage:

  (a) Start chrome with the default options from a default binary location
  (start-chrome)

  (b) Start chrome using the given chromedriver with the default chrome options
  (start-chrome \"/usr/lib/chromium-dev/chromedriver\")

  (c) Start chrome using a given chromedriver with list of specific chrome-options :

  (start-chrome \"/usr/lib/chromium/chromedriver\"
                ;; Arguments for init-chrome-options above.
                \"headless\"                 ;; Run headless (omit to run non-headless)
                \"--disable-gpu\"            ;; mandatory argument for Chrome/Chromium 59.0
                \"--window-size=1920,1080\") ;; optional but good to have
  For complete options please see https://goo.gl/DcUcrj"
  ([]
   (start-chrome (get-chromedriver-binary)))
  ([chromedriver-binary]
   (start-chrome chromedriver-binary))
  ([chromedriver-binary & args]
   (System/setProperty driver-prop (get-chromedriver-binary chromedriver-binary))
   (if args
     (instance (init-chrome-options args))
     (instance))))