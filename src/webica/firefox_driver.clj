(ns webica.firefox-driver
  "Clojure binding for Selenium class FirefoxDriver"
  (:refer-clojure :exclude [get])
  (:require [clojure.core :as clj]
            [webica.core :as w]
            [me.raynes.fs :as fs])
  (:import [org.openqa.selenium.firefox
            FirefoxDriver]))

(w/intern-java FirefoxDriver *ns*
  {:clear '[quit kill]})

(defn start-firefox [&[geckodriver]]
  (let [driver-prop "webdriver.firefox.driver"
        default-exe (System/getProperty driver-prop)
        geckodriver-system "/usr/bin/geckodriver"
        geckodriver-local "/usr/local/bin/geckodriver"
        geckodriver (or geckodriver
                     (if (and (not (empty? default-exe))
                           (fs/exists? default-exe))
                       default-exe)
                     (if (fs/exists? geckodriver-system)
                       geckodriver-system)
                     geckodriver-local)]
    (if-not (fs/exists? geckodriver)
      (throw
        (RuntimeException.
          (str "ERROR: geckodriver executable not found:" geckodriver))))
    (System/setProperty driver-prop geckodriver)
    (instance)))