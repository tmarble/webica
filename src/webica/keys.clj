(ns webica.keys
  "Clojure binding for Selenium class Keys"
  (:refer-clojure :exclude [name])
  (:require [clojure.core :as clj]
            [webica.core :as w])
  (:import [org.openqa.selenium
            Keys]))

(w/intern-java Keys *ns*
  {:coercions
   [["org.openqa.selenium.Keys" "java.lang.CharSequence" identity]]})

