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