(ns webica.web-driver-wait
  "Clojure binding for Selenium class WebDriverWait"
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium.support.ui
            WebDriverWait]))

(w/intern-java WebDriverWait *ns*
  {:coercions
   [["webica.web_driver_wait.Condition" "com.google.common.base.Function" identity]]})

(deftype Condition [apply-fn]
  org.openqa.selenium.support.ui.ExpectedCondition
  (apply [this driver]
    (apply-fn driver)))

(defn condition
  "Create a condition for wait. For example,
  (condition
    (fn [driver]
      (string/starts-with?
        (string/lower-case (driver/get-title driver))
        'cheese!'))))"
  [apply-fn]
  (Condition. apply-fn))

(def ^:dynamic *default-timeout* 15000) ;; msec

(defn init-web-driver-wait
  "Create the WebDriverWait from existing WebDriver object using a given timeout (in msec)."
  ([driver]
    (init-web-driver-wait driver *default-timeout*))
  ([driver timeout]
   ;; Set implicitly for a given timeout
   (let [timeout-in-secs (/ timeout 1000)]
     (.implicitlyWait
       (-> driver .manage .timeouts)
       timeout-in-secs java.util.concurrent.TimeUnit/SECONDS)
   ;; Then return the WebDriverWait instance
   (org.openqa.selenium.support.ui.WebDriverWait. driver timeout-in-secs))))

(defn until
  "Wait until a given condition is met or raise exception if the condition can't be met.

  Sample usage:
  ;; a) For the most control try
  (until wdriver (ec/title-contains \"some-text\"))

  ;; b) For simple condition with one argument :
  (until ec/title-is \"Google Search - Clojure Conj 2016\")

  ;; c) For condition that needed require `by` locator e.g. by/xpath, by/css-selector, etc:
  (until ec/presence-of-element-located by/xpath \"some-id\")"

  ([wdriver expected-fn]
   (.until wdriver expected-fn))
  ([wdriver expected-fn arg]
   (.until wdriver (expected-fn arg)))
  ([wdriver expected-fn by-fn arg]
   (.until wdriver (expected-fn (by-fn arg)))))