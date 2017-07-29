;; Copyright (c) 2016 Tom Marble
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns webica.core
  "Clojure wrapper for Selenium Webdriver

NOTE: load this namespace first when using webica."
  (:require [clojure.string :as string]
            [clojure.pprint :as pp :refer [pprint]]
            [environ.core :refer [env]]
            [camel-snake-kebab.core :as translate]
            [avenir.utils :refer [assoc-if concatv]]
            [taoensso.timbre :as log])
  (:import [clojure.lang
            Reflector]
           [java.lang.reflect
            InvocationTargetException ParameterizedType Modifier Type]
           [com.google.common.collect
            ImmutableList ImmutableMap]
           [org.openqa.selenium
            By Keys WebDriver WebElement]
           [org.openqa.selenium.chrome
            ChromeDriver ChromeDriverService]
           [org.openqa.selenium.firefox
            FirefoxDriver]
           [org.openqa.selenium.interactions
            Keyboard]
           [org.openqa.selenium.remote
            RemoteWebDriver RemoteWebElement]
           [org.openqa.selenium.support.ui
            WebDriverWait
            ExpectedConditions]))

;; yes, the sleep function really wants to live somewhere else
(defn sleep
  "sleeps for the given number of seconds (may be fractional)"
  {:added "clj0"}
  [s]
  (Thread/sleep (* 1000 s)))

;; constant that is used during Clojure source code generation
(def generated? false)

;; known data type coercions
;; {to {from f}}
;; when coercing from 'from' to 'to' use f
(defonce ^:dynamic *coercions* (atom {}))

;; known Selenium classes, e.g.
;; {"org.openqa.selenium.firefox.FirefoxDriver"
;;  {:java-class org.openqa.selenium.firefox.FirefoxDriver,
;;   :type-name "org.openqa.selenium.firefox.FirefoxDriver",
;;   :class-decl :normal,
;;   :instance nil,
;;   :constructors
;;   [#object[java.lang.reflect.Constructor 0x16e0e638 "public org.openqa.selenium.firefox.FirefoxDriver(org.openqa.selenium.firefox.FirefoxBinary,org.openqa.selenium.firefox.FirefoxProfile,org.openqa.selenium.Capabilities)"]
;;    #object[java.lang.reflect.Constructor 0x2da58399 "public org.openqa.selenium.firefox.FirefoxDriver(org.openqa.selenium.firefox.FirefoxBinary,org.openqa.selenium.firefox.FirefoxProfile)"]],
;;   :zero-arg-constructor
;;   #object[java.lang.reflect.Constructor 0x7b23c790 "public org.openqa.selenium.firefox.FirefoxDriver()"],
;;   :interfaces
;;   ["org.openqa.selenium.internal.Killable"
;;    "org.openqa.selenium.WebDriver"
;;    "org.openqa.selenium.JavascriptExecutor"
;;    "org.openqa.selenium.internal.FindsById"
;;    "org.openqa.selenium.internal.FindsByClassName"
;;    "org.openqa.selenium.internal.FindsByLinkText"
;;    "org.openqa.selenium.internal.FindsByName"
;;    "org.openqa.selenium.internal.FindsByCssSelector"
;;    "org.openqa.selenium.internal.FindsByTagName"
;;    "org.openqa.selenium.internal.FindsByXPath"
;;    "org.openqa.selenium.interactions.HasInputDevices"
;;    "org.openqa.selenium.HasCapabilities"
;;    "org.openqa.selenium.TakesScreenshot"],
;;   :super-class "org.openqa.selenium.remote.RemoteWebDriver",
;;   :methods
;;   {"set-error-handler"
;;    [{:method-name "setErrorHandler",
;;      :method
;;      #object[java.lang.reflect.Method 0x435c63af "public void org.openqa.selenium.remote.RemoteWebDriver.setErrorHandler(org.openqa.selenium.remote.ErrorHandler)"],
;;      :access :public,
;;      :static? false,
;;      :variadic? false,
;;      :params ["org.openqa.selenium.remote.ErrorHandler"],
;;      :rt "void",
;;      :clear? nil}]}}}
(defonce ^:dynamic *java-classes* (atom {}))

(def #^{:added "clj0"}
  web-driver-type-name
  "Common interface for actual browser drivers"
  "org.openqa.selenium.WebDriver")

(def #^{:added "clj0"}
  remote-web-driver-type-name
  "Common interface for actual browser drivers"
  "org.openqa.selenium.remote.RemoteWebDriver")

(def #^{:added "clj0"}
  chrome-driver-extra
  "Extra functions for the chrome-driver ns"
  "
(defn ^:private init-chrome-options
  \"The default ChromeOptions with the web-driver initialization.
  e.g.
  (init-chrome-options \\\"headless\\\"
                       \\\"--disable-gpu\\\"
                       \\\"--window-size=1920,1080\\\")\"
  [& args]
  (let [chrome-options (org.openqa.selenium.chrome.ChromeOptions.)
        flags (vec (first args))]
    ;; Call ChromeOptions.addArguments()
    (.addArguments chrome-options flags)
    chrome-options))

(defn start-chrome [chromedriver & args]
  \"The default chrome options with the web-driver initialization.

  Example Usage:

  (a) Create chrome-driver without chrome-options :

  (start-chrome \\\"/usr/lib/chromium/chromedriver\\\")

  (b) Create chrome-driver with chrome-options :

  (start-chrome \\\"/usr/lib/chromium/chromedriver\\\"
                ;; Arguments for init-chrome-options above.
                \\\"headless\\\"                 ;; Run headless (omit to run non-headless)
                \\\"--disable-gpu\\\"            ;; mandatory argument for Chrome/Chromium 59.0
                \\\"--window-size=1920,1080\\\") ;; optional but good to have
  For complete options please see https://goo.gl/DcUcrj\"
  (let [driver-prop \"webdriver.chrome.driver\"
        default-exe (System/getProperty driver-prop)
        chromedriver-system \"/usr/lib/chromium/chromedriver\"
        chromedriver-local \"/usr/local/bin/chromedriver\"
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
          (str \"ERROR: chromedriver executable not found:\" chromedriver))))
    (System/setProperty driver-prop chromedriver)
    (if args
      (instance (init-chrome-options args))
      (instance))))")

(def #^{:added "clj0"}
  firefox-driver-extra
  "Extra functions for the firefox-driver ns"
  "(defn start-firefox [&[geckodriver]]
  (let [driver-prop \"webdriver.firefox.driver\"
        default-exe (System/getProperty driver-prop)
        geckodriver-system \"/usr/bin/geckodriver\"
        geckodriver-local \"/usr/local/bin/geckodriver\"
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
          (str \"ERROR: geckodriver executable not found:\" geckodriver))))
    (System/setProperty driver-prop geckodriver)
    (instance)))")

(def #^{:added "clj0"}
  by-coercions
  "Coercions for the by ns"
  '[["org.openqa.selenium.By$ByClassName" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByCssSelector" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ById" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByLinkText" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByName" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByPartialLinkText" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByTagName" "org.openqa.selenium.By" identity]
    ["org.openqa.selenium.By$ByXPath" "org.openqa.selenium.By" identity]])

(def #^{:added "clj0"}
  keyboard-coercions
  "Coercions for the keyboard ns"
  '[["org.openqa.selenium.remote.RemoteKeyboard"
     "org.openqa.selenium.interactions.Keyboard"
     identity]
    ["org.openqa.selenium.events.internal.EventFiringKeyboard"
     "org.openqa.selenium.interactions.Keyboard"
     identity]])

(def #^{:added "clj0"}
  keys-coercions
  "Coercions for the keys ns"
  '[["org.openqa.selenium.Keys" "java.lang.CharSequence" identity]])

(def #^{:added "clj0"}
  web-driver-wait-extra
  "Extra functions for the web-driver-wait ns"
  "(deftype Condition [apply-fn]
  org.openqa.selenium.support.ui.ExpectedCondition
  (apply [this driver]
    (apply-fn driver)))

(defn condition
  \"Create a condition for wait. For example,
  (condition
    (fn [driver]
      (string/starts-with?
        (string/lower-case (driver/get-title driver))
        'cheese!'))))\"
  [apply-fn]
  (Condition. apply-fn))")

(def #^{:added "clj0"}
  web-driver-wait-coercions
  "Coercions for the web-driver-wait ns"
  '[["webica.web_driver_wait.Condition"
     "com.google.common.base.Function"
     identity]])

(def #^{:added "clj0"}
  selenium-classes
  "These are the Selenium classes webica will know about including hints
on how to generate the Clojure source code."
  {By {:exclude '[name]
       :coercions by-coercions}
   ChromeDriver {:exclude '[get]
                 :clear '[quit kill]
                 :require '[[me.raynes.fs :as fs]]
                 :extra chrome-driver-extra}
   ChromeDriverService {}
   FirefoxDriver {:exclude '[get]
                  :clear '[quit kill]
                  :require '[[me.raynes.fs :as fs]]
                  :extra firefox-driver-extra}
   Keyboard {:coercions keyboard-coercions}
   Keys {:exclude '[name]
         :coercions keys-coercions}
   RemoteWebDriver {:exclude '[get]
                    :clear '[quit kill]}
   RemoteWebElement {}
   WebDriver {:exclude '[get]}
   WebDriverWait {:extra web-driver-wait-extra
                  :coercions web-driver-wait-coercions}
   ExpectedConditions {:exclude '[not or and]}
   WebElement {}})

(def #^{:added "clj0"}
  clojure-type-names
  "Java type name equivalences for common Clojure data types"
  {"char" "java.lang.Character"
   "str" "java.lang.String"
   "int" "java.lang.Integer"
   "long" "java.lang.Long"
   "boolean" "java.lang.Boolean"
   "double" "java.lang.Double"
   "float" "java.lang.Float"
   "bigdec" "java.math.BigDecimal"})

(def #^{:added "clj0"}
  log-levels
  "All possible logging levels"
  #{"trace" "debug" "info" "warn" "error" "fatal" "report"})

(def #^{:added "clj0"}
  log-config
  "Timbre logging configuration map"
  {:timestamp-opts   {:pattern  "yy-MMM-dd HH:mm:ss.SSS"
                      :locale   (java.util.Locale. "en")
                      :timezone (java.util.TimeZone/getDefault)
                      ;; (java.util.TimeZone/getTimeZone "UTC")
                      }
   :output-fn log/default-output-fn ; (fn [data]) -> string
   })

(defn initialize-logging
  "Starts the logging infrastructure
Set WEBICA_LOG to the filename of the logfile [webica.log]
and WEBICA_LEVEL to the desired logging level [warn]."
  {:added "clj0"}
  []
  (let [log (or (:webica-log env) "webica.log")
        default-level "warn"
        webica-level (:webica-level env)
        level (keyword (or (and webica-level (log-levels webica-level))
                         default-level))
        appenders {:spit (log/spit-appender {:fname log})}
        config (assoc log-config
                 :level level
                 :appenders appenders)]
    (log/set-config! config)
    (log/warn "webica logging initialized at level " level)))

(defn webica-namespaces
  "Returns a vector of webica namespaces"
  {:added "clj0"}
  []
  (let [wcls (keys selenium-classes)]
    (loop [wns [] c (first wcls) more (rest wcls)]
      (if-not c
        wns
        (let [selenium-class-name (.getSimpleName c)
              clj-ns-name (translate/->kebab-case selenium-class-name)
              clj-ns (symbol (str "webica." clj-ns-name))]
          (recur (conj wns clj-ns) (first more) (rest more)))))))

(defn get-java-class-info
  "Retrieves a portion of the **java-classes** (like get-in ks)"
  {:added "clj0"}
  [& ks]
  (if (pos? (count ks))
    (get-in @*java-classes* (vec ks))
    (throw (IllegalArgumentException.
             "get-java-class-info must have at least one key specified"))))

(defn get-type-name
  "Return a string representing the type of x"
  {:added "clj0"}
  [x]
  (let [type-name
        (if (nil? x)
          "nil"
          (if (instance? Type x)
            (.getTypeName x)
            (if (instance? Class x)
              (.getName x)
              (.getName (class x)))))]
    (get clojure-type-names type-name type-name)))

(defn type-name?
  "Returns true if x a type-name"
  {:added "clj0"}
  [x]
  (boolean
    (and (string? x)
      (or (= x "nil")
        (re-matches #"(\[L)?([a-z]+\.)*[A-Z][A-Za-z\$]+(\[\])?;?" x)))))

(defn selenium-package?
  "Returns true if java-class is part of Selenium"
  {:added "clj0"}
  [java-class]
  (let [type-name (get-type-name java-class)]
    (if (re-find #"org\.openqa\.selenium" type-name)
      true
      false)))

(defn zero-arg-constructor?
  "Returns true if constructor is public and takes no arguments."
  {:added "clj0"}
  [constructor]
  (and (Modifier/isPublic (.getModifiers constructor))
    (zero? (.getParameterCount constructor))))

(defn get-class-decl
  "Returns a keyword for the type of java-class, one of :interface :enum or :normal"
  {:added "clj0"}
  [java-class]
  (if (.isInterface java-class)
    :interface
    (if (.isEnum java-class)
      :enum
      :normal)))

(defn get-access
  "Returns a keyword for the modifiers, one of :public :private or :protected"
  {:added "clj0"}
  [modifiers]
  (if (Modifier/isPublic modifiers)
    :public
    (if (Modifier/isProtected modifiers)
      :protected
      :private)))

(defn add-coercion
  "Adds a known coercion from type-name 'from' to type-name 'to' using function f"
  {:added "clj0"}
  [from to f]
  (swap! *coercions* assoc-in [to from] f))

(defn string->char-sequence
  "Coercion from string to char-sequence"
  {:added "clj0"}
  [s]
  (into-array java.lang.CharSequence [s]))

(defn empty-object-array
  "Returns an empty Object array"
  {:added "clj0"}
  [x]
  (into-array Object nil))

;; clojure.lang.PersistentVector => com.google.common.collect.ImmutableList
;; com.google.common.collect.ImmutableList<java.lang.String>
;; https://google.github.io/guava/releases/19.0/api/docs/com/google/common/collect/package-summary.html
(defn immutable-list
  "Coercion from vector to ImmutableList"
  {:added "clj0"}
  [v]
  (let [builder (ImmutableList/builder)
        _ (doseq [e v] (.add builder e))
        rv (.build builder)]
    rv))

;; clojure.lang.PersistentArrayMap => com.google.common.collect.ImmutableMap
(defn immutable-map
  "Coercion from map to ImmutableMap"
  {:added "clj0"}
  [m]
  (let [builder (ImmutableMap/builder)
        _ (doseq [[k v] m] (.put builder k v))
        rv (.build builder)]
    rv))

(defn initialize-coercions
  "Initialize *coercions* and add fundamental types"
  {:added "clj0"}
  []
  (reset! *coercions* {})
  (add-coercion "nil" "java.lang.Boolean" boolean)
  (add-coercion "nil" "java.lang.Object[]" empty-object-array)
  (add-coercion "java.lang.String" "java.lang.CharSequence[]"
    string->char-sequence)
  (add-coercion "java.lang.String" "java.lang.CharSequence" identity)
  (add-coercion "java.lang.Long" "java.lang.Integer" int)
  (add-coercion "clojure.lang.PersistentVector"
    "com.google.common.collect.ImmutableList" immutable-list)
  (add-coercion "clojure.lang.PersistentArrayMap"
    "com.google.common.collect.ImmutableMap" immutable-map))

(defn initialize-java-classes
  "Loads all webica namespaces such that all the coercions are available"
  {:added "clj0"}
  []
  (doseq [ns (webica-namespaces)]
    (require ns))
  (log/debug "webica knows about" (count @*java-classes*) "selenium classes"))

(defn coercible
  "Returns a function to coerce from-type to to-type (if possible, else nil)"
  {:added "clj0"}
  [from to]
  (let [from-type (if (type-name? from) from (get-type-name from))
        to-type (if (type-name? to) to (get-type-name to))
        coerce-fn (if (= from-type to-type)
                    identity
                    (get-in @*coercions* [to-type from-type]))
        coerce-fn (if (and (not coerce-fn) (= to-type "java.lang.String"))
                    str coerce-fn)]
    (if-not coerce-fn
      (log/debug "NOT coercible" from-type "=>" to-type)
      (log/trace "OK" from-type "=>" to-type))
    coerce-fn))

(defn coerce
  "Coerce value form from-type to to-type (if possible, else exception)"
  {:added "clj0"}
  [from to value & [call-site]]
  (let [from-type (if (type-name? from) from (get-type-name from))
        to-type (if (type-name? to) to (get-type-name to))
        coerce-fn (coercible from-type to-type)]
    (if coerce-fn
      (coerce-fn value)
      (let [err (str "No coercion is available to turn \""
                  from-type "\" into an object of \""
                  to-type "\""
                  (if call-site " for ") call-site)]
        (throw (IllegalArgumentException. err))))))

(defn best-constructor
  "Dynamically pick the best constructor given the args types"
  {:added "clj0"}
  [java-class-info args]
  (let [{:keys [type-name constructors]} java-class-info
        [ctor arg-arr]
        (loop [ctor-arg-arr nil c (first constructors) more (rest constructors)]
          (if ctor-arg-arr
            ctor-arg-arr
            (if-not c
              (throw
                (IllegalArgumentException.
                  (str "No matching version of the \"" type-name
                    "\" constructor matches the arguments provided")))
              (let [params (.getParameterTypes c)
                    param0 (first params)
                    param0 (if param0 (get-type-name param0))
                    p (count params)
                    n (count args)
                    param0-wd? (and (pos? p)
                                 (= (inc n) p)
                                 (#{web-driver-type-name
                                    remote-web-driver-type-name}
                                   param0))
                    wd (get-java-class-info
                         remote-web-driver-type-name :instance)
                    [coerce-fns args] (if (= n p)
                                        [(map coercible args params) args]
                                        (if (and param0-wd? wd)
                                          [(cons identity
                                             (map coercible args
                                               (rest params)))
                                           (cons wd args)]))
                    arg-arr (if (= p (count (remove nil? coerce-fns)))
                              (into-array Object (map #(%1 %2) coerce-fns args)))
                    ctor-arg-arr (if arg-arr [c arg-arr])]
                (recur ctor-arg-arr (first more) (rest more))))))]
    (if ctor
      (fn []
        (.newInstance ctor arg-arr)))))

(defn create-instance-fns
  "Create functions to create an instance of this class **create-instance**,
Return the value of the current instance **get-instance**,
Lazily create an instance **instance**, and
Delete the instance **clear-instance**."
  {:added "clj0"}
  [java-class-info ns]
  (let [{:keys [type-name java-class class-decl zero-arg-constructor]} java-class-info]
    (intern ns
      (with-meta 'get-instance
        {:name 'get-instance
         :argslists '([])
         :doc (str "Returns current instance (or nil) for " type-name)})
      (fn []
        (let [java-class-info (get-java-class-info type-name)
              instance (:instance java-class-info)]
          instance)))
    (intern ns
      (with-meta 'clear-instance
        {:name 'clear-instance
         :argslists '([])
         :doc (str "Remove any active instance of " type-name)})
      (fn []
        (let [java-class-info (get-java-class-info type-name)
              instance (:instance java-class-info)
              web-driver-instance (get-java-class-info
                                    web-driver-type-name :instance)
              remote-web-driver-instance (get-java-class-info
                                           remote-web-driver-type-name :instance)]
          (when instance
            (swap! *java-classes* assoc-in [type-name :instance] nil)
            (when (= web-driver-instance instance)
              (log/trace "clear web-driver-instance")
              (swap! *java-classes* assoc-in
                [web-driver-type-name :instance] nil))
            (when (= remote-web-driver-instance instance)
              (log/trace "clear remote-web-driver-instance")
              (swap! *java-classes* assoc-in
                [remote-web-driver-type-name :instance] nil)))
          nil)))
    (intern ns
      (with-meta 'instance
        {:name 'instance
         :argslists '([& args])
         :doc (str "Lazy constructor for " type-name
                "\n  (will only create an instance as needed)")})
      (fn [& args]
        (let [java-class-info (get-java-class-info type-name)
              {:keys [instance type-name super-class]} java-class-info]
          (log/trace "instance of" type-name "=" instance)
          (if (empty? args)
            (if instance
              instance
              (if zero-arg-constructor
                (let [instance (or instance
                                 (.newInstance zero-arg-constructor
                                   (make-array Object 0)))]
                  (swap! *java-classes* assoc-in [type-name :instance]
                    instance)
                  (when (#{web-driver-type-name remote-web-driver-type-name}
                          super-class) ;; save web-driver
                    (log/trace "SAVING this instance of" type-name
                      "for" super-class)
                    (swap! *java-classes* assoc-in
                      [web-driver-type-name :instance] instance)
                    (swap! *java-classes* assoc-in
                      [remote-web-driver-type-name :instance] instance))
                  instance)
                (throw (RuntimeException.
                         (if (= class-decl :enum)
                           (str "Please use one of the enums as an argument for non-static functions in " type-name)
                           (str "There is no zero-arg-constructor for " type-name))))))
            (let [ctor (best-constructor java-class-info args)
                  instance (if ctor (ctor))]
              (when-not instance
                (throw (RuntimeException.
                         (str "Unable to find a suitable constructor for "
                           type-name))))
              (when instance
                (when (#{web-driver-type-name remote-web-driver-type-name}
                        super-class) ;; save web-driver
                  (log/trace "SAVING this instance of" type-name
                    "for" super-class)
                  (swap! *java-classes* assoc-in
                    [web-driver-type-name :instance] instance)
                  (swap! *java-classes* assoc-in
                    [remote-web-driver-type-name :instance] instance))
                instance))))))))


;; returns [method this arg-arr clear?] when a method has been matched
;; else throws an exception
;; method is the actual method chosen
;; this is the instance of the class to use (or nil if static?)
;; arg-arr is the array of argument objects ready for invocation
;; when clear? the function clear-instance should be called after this fn
(defn match-fn
  "Dynamically match function based on argument types"
  {:added "clj0"}
  [type-name class-decl instance fname ms args]
  (loop [match nil m (first ms) more (rest ms)]
    (if match
      match
      (if-not m
        (throw
          (IllegalArgumentException.
            (str "No matching version of the \"" fname
              "\" function matches the arguments provided")))
        (let [{:keys [method-name method static? variadic? params rt clear?]} m
              ;; need-instance? (or (and (= class-decl :normal) (not static?))
              ;;                  (= class-decl :interface))
              need-instance? (not static?)
              arg0-this? (and need-instance?
                           (pos? (count args))
                           (= (inc (count params)) (count args))
                           (coercible (first args) type-name))
              [this args] (if arg0-this?
                            [(first args) (rest args)]
                            [(if need-instance? (instance)) args])
              coerce-fns (map coercible args params)
              arg-arr (if (= (count params) (count (remove nil? coerce-fns)))
                        (into-array Object (map #(%1 %2) coerce-fns args)))
              match (if arg-arr [method this arg-arr clear?])]
          (recur match (first more) (rest more)))))))

(defn create-method-fn
  "Create a Clojure function for a Selenium method"
  {:added "clj0"}
  [java-class-info fname ms ns]
  (let [fsym (symbol fname)
        {:keys [java-class type-name class-decl]} java-class-info
        instance (ns-resolve ns 'instance)
        [argslists doc]
        (loop [argslists [] doc nil m (first ms) more (rest ms)]
          (if-not m
            [(apply list argslists) doc]
            (let [{:keys [method-name method static? variadic? params rt clear?]} m
                  argslist (vec (for [i (range (count params))]
                                  (symbol (str "arg" i))))
                  argslist (if variadic? (concatv argslist '[& ...]) argslist)
                  argslists (conj argslists argslist)
                  invocation (if static?
                               (str type-name "/" method-name)
                               (str "." method-name " (instance)"))
                  d (apply str "(" invocation
                      (for [i (range (count params)) :let [param (get params i)]]
                        (str " ^" param " arg" i)))
                  d (str d ")\n    => ^" rt)
                  doc (if doc (str doc "\n  " d) d)]
              (recur argslists doc (first more) (rest more)))))]
    (intern ns
      (with-meta fsym
        {:name fsym
         :argslists argslists
         :doc doc})
      (fn [& args]
        (let [[method this arg-arr clear?]
              (match-fn type-name class-decl instance fname ms args)
              _ (log/trace "CALL" fname "THIS" this "args" args)
              rv (.invoke method this arg-arr)]
          (log/trace "RV" rv)
          (if clear?
            ((ns-resolve ns 'clear-instance)))
          rv)))))

(defn create-enum-fns
  "Create Clojure functions for a Selenium enum class constants"
  {:added "clj0"}
  [java-class-info ns]
  (let [{:keys [type-name]} java-class-info]
    (intern ns
      (with-meta 'enum-name
        {:name 'enum-name
         :argslists '([enum])
         :doc (str "Returns name for the enum in " type-name)})
      (fn [enum]
        (.name enum)))
    (intern ns
      (with-meta 'ordinal
        {:name 'ordinal
         :argslists '([enum])
         :doc (str "Returns name for the ordinal for this enum in the values of " type-name)})
      (fn [enum]
        (.ordinal enum)))))

(defn create-enum
  "Create Clojure def for a Selenium enum"
  {:added "clj0"}
  [java-class-info enum ns]
  (let [ename (.name enum)
        esym (symbol ename)
        doc (str "Enum " ename " for the class " (:type-name java-class-info))]
    (intern ns
      (with-meta esym
        {:name esym
         :doc doc})
      enum)))

(defn get-java-method
  "Return information about the given Java method"
  {:added "clj0"}
  [method clear-fns]
  (let [method-name (.getName method)
        modifiers (.getModifiers method)
        access (get-access modifiers)
        static? (boolean (Modifier/isStatic modifiers))
        variadic? (boolean (.isVarArgs method))
        params (mapv get-type-name (.getParameterTypes method))
        rt (get-type-name (.getGenericReturnType method))
        ;; boolean? (boolean (or (= rt "boolean") (= rt "java.lang.Boolean")))
        boolean? (boolean (#{"boolean" "java.lang.Boolean"} rt))
        ;; boolean? (boolean false)
        fname (str (translate/->kebab-case method-name) (if boolean? "?"))
        fsym (symbol fname)
        clear? (some #(= fsym %) clear-fns)]
    (if (= access :public)
      [fname
       {:method-name method-name
        :method method
        :access access
        :static? static?
        :variadic? variadic?
        :params params
        :rt rt
        :clear? clear?}])))

(defn get-java-methods
  "Return information about the Java methods in java-class"
  {:added "clj0"}
  [java-class clear-fns]
  (let [methods (.getDeclaredMethods java-class)
        methods (reduce
                  (fn [method-map fname-method]
                    (let [[fname method] fname-method]
                      (if (contains? method-map fname)
                        (update-in method-map [fname] conj method)
                        (assoc method-map fname [method]))))
                  {}
                  (remove nil? (for [method methods]
                                 (get-java-method method clear-fns))))
        super-class (.getSuperclass java-class)
        super-class (if (and super-class (selenium-package? super-class))
                      super-class)
        methods (if super-class
                  (merge (get-java-methods super-class clear-fns) methods)
                  methods)]
    methods))

(defn get-all-interfaces
  "Return information about the Java interfaces in java-class"
  {:added "clj0"}
  [java-class]
  (let [interfaces (vec (.getInterfaces java-class))
        super-class (.getSuperclass java-class)]
    (if (and super-class (selenium-package? super-class))
      (concatv interfaces (get-all-interfaces super-class))
      interfaces)))

(defn create-java-class-info
  "Return a map with information about the java-class"
  {:added "clj0"}
  [java-class clear-fns]
  (let [type-name (get-type-name java-class)
        class-decl (get-class-decl java-class)
        constructors (if (= class-decl :normal)
                       (.getConstructors java-class))
        zero-arg-constructor (first (filter zero-arg-constructor? constructors))
        interfaces (mapv get-type-name (get-all-interfaces java-class))
        enums (if (= class-decl :enum)
                (.getEnumConstants java-class))
        super-class (.getSuperclass java-class)
        super-class (if (and super-class (selenium-package? super-class))
                      (get-type-name super-class))
        methods (get-java-methods java-class clear-fns)]
    (when super-class
      (add-coercion type-name super-class identity))
    (when interfaces
      (doseq [interface interfaces]
        (add-coercion type-name interface identity)))
    (assoc-if {:java-class java-class
               :type-name type-name
               :class-decl class-decl
               :instance nil}
      :constructors constructors
      :zero-arg-constructor zero-arg-constructor
      :interfaces interfaces
      :enums enums
      :super-class super-class
      :methods methods)))

(defn show-functions
  "Create the show-functions function in the given ns"
  {:added "clj0"}
  [ns]
  (let [namespace (ns-name ns)
        publics (sort-by :name (map meta (vals (ns-publics ns))))
        doc (str "Symbols in " namespace)]
    (intern ns
      (with-meta 'show-functions
        {:name 'show-functions
         :argslists '([])
         :doc (str "Show symbols in " namespace)})
      (fn []
        (println doc)
        (println (apply str (repeat (count doc) \-)))
        (doseq [public publics]
          (println (name (:name public)) "\n " (:doc public) \newline))))))

(defn intern-java
  "Intern into the specified namespace all public methods
   from the java-class as Clojure functions."
  {:added "clj0"}
  [java-class ns & [{:keys [coercions clear] :as opts}]]
  (show-functions ns)
  (doseq [coercion coercions]
    (let [[from-type to-type coerce-fn] coercion]
      (add-coercion from-type to-type coerce-fn)))
  (let [java-class-info (create-java-class-info java-class clear)
        {:keys [type-name class-decl methods enums]} java-class-info]
    (swap! *java-classes* assoc type-name java-class-info)
    (create-instance-fns java-class-info ns)
    (doseq [[fname methods-info] methods]
      (create-method-fn java-class-info fname methods-info ns))
    (when (= class-decl :enum)
      (create-enum-fns java-class-info ns)
      (doseq [enum enums]
        (create-enum java-class-info enum ns)))))

;; Initialize data structures on first load
(when (and generated? (empty? @*coercions*))
  (initialize-logging)
  (initialize-coercions)
  (initialize-java-classes))
