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

(defn init-webdriver-wait
  "Create the WebDriverWait from existing WebDriver object."
  ([driver]
    (init-webdriver-wait driver 10))
  ([driver max-timeouts]
    ;; Always maximize window
    (-> driver
        .manage
        .window
        .maximize)
   ;; Manage the implicit timeouts
   (.implicitlyWait
     (-> driver
         .manage
         .timeouts)
     max-timeouts java.util.concurrent.TimeUnit/SECONDS)
   (let [wait-instance (org.openqa.selenium.support.ui.WebDriverWait. driver max-timeouts)]
     wait-instance)))

(defn wait-until
  "Wait until a given condition is met or raise exception if the condition can't be met.

  Example:
  (wait-until presence-of-element-located
              xpath
              \"some-id\")"
  [wdriver expected-cond-fn by-fn arg]
  (.until wdriver (expected-cond-fn (by-fn arg))))
