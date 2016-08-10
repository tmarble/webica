(ns webica.keyboard
  "Clojure binding for Selenium class Keyboard"
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium.interactions
            Keyboard]))

(w/intern-java Keyboard *ns*
  {:coercions
   [["org.openqa.selenium.remote.RemoteKeyboard" "org.openqa.selenium.interactions.Keyboard" identity]
    ["org.openqa.selenium.events.internal.EventFiringKeyboard" "org.openqa.selenium.interactions.Keyboard" identity]]})

