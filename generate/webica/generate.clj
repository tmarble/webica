;; Copyright (c) 2016 Tom Marble
;;
;; This software is licensed under the terms of the
;; Apache License, Version 2.0 which can be found in
;; the file LICENSE at the root of this distribution.

(ns webica.generate
  "Generation functions to make Clojure wrapper for Selenium WebDriver"
  (:require [clojure.string :as string]
            [clojure.java.shell :refer [sh]]
            [pom-versions.core :refer [get-versions] :as pv]
            [camel-snake-kebab.core :as translate]
            [webica.core :as w]
            [me.raynes.fs :as fs]))

(def #^{:added "clj0"}
  selenium-maven-url
  "Repository URL to determine published Selenium versions"
  "http://repo1.maven.org/maven2/org/seleniumhq/selenium/selenium-java/maven-metadata.xml")

(defn get-selenium-versions
  "Returns a vector of known Selenium versions (in order)"
  {:added "clj0"}
  ([]
   (pv/get-versions (slurp selenium-maven-url))))

(defn- pprint-coercions [coercions]
  (let [n (count coercions)]
    (apply str
      (for [i (range n)
            :let [coercion (nth coercions i)
                  [from to f] coercion]]
        (str
          (if (zero? i) "   [" "    ")
          "[\"" from "\" \"" to "\" "
          f
          "]"
          (if (= i (dec n)) "]" "\n"))))))

(defn- create-clj-src [src-webica selenium-class]
  (let [{:keys [exclude require clear coercions extra]}
        (get w/selenium-classes selenium-class)
        selenium-class-name (.getSimpleName selenium-class)
        selenium-pkg-name (.getName (.getPackage selenium-class))
        clj-ns-name (translate/->kebab-case selenium-class-name)
        clj-src (fs/file src-webica
                  (str (translate/->snake_case selenium-class-name) ".clj"))
        _ (println "creating clojure source:" (fs/base-name clj-src))
        ns-doc (str "Clojure binding for Selenium class " selenium-class-name)
        ns-src (str "(ns webica." clj-ns-name "\n  \"" ns-doc "\"\n"
                 (if exclude (str "  (:refer-clojure :exclude " exclude ")\n"))
                 "  (:require [clojure.core :as clj]\n"
                 "            [webica.core :as w]"
                 (if require
                   (apply str
                     (for [r require]
                       (str "\n            " r))))
                 ")\n"
                 "  (:import [" selenium-pkg-name "\n            "
                 selenium-class-name "]))\n\n"
                 )
        intern-src (str "(w/intern-java " selenium-class-name " *ns*"
                     (if (or clear coercions) "\n  {")
                     (if clear (str ":clear '" clear))
                     (if coercions
                       (str (if clear "\n   ")
                         ":coercions\n"
                         (pprint-coercions coercions)))
                     (if (or clear coercions) "}")
                     ")\n\n"
                     extra)
        src (str ns-src intern-src)]
    (spit clj-src src)))

(defn- create-webica-core [src-webica-core generate-webica-core]
  (let [generate-core (slurp generate-webica-core)
        src-core (-> generate-core
                   ;; (string/replace
                   ;;   #"\(def all-coercions \{\}\)\n"
                   ;;   (pprint-coercions))
                   (string/replace
                     "(def generated? false)\n"
                     "(def generated? true)\n"))]
    (spit src-webica-core src-core)))

(defn- update-webica []
  (let [src-webica (fs/file "." "src/webica")
        src-webica-prev (fs/file "." "src/webica.prev")
        src-webica-core (fs/file "." "src/webica/core.clj")
        generate-webica-core (fs/file "." "generate/webica/core.clj")]
    (if (fs/exists? src-webica-prev)
      (fs/delete-dir src-webica-prev))
    (if (fs/exists? src-webica)
      (fs/rename src-webica src-webica-prev))
    (fs/mkdirs src-webica)
    (doseq [selenium-class (keys w/selenium-classes)]
      (create-clj-src src-webica selenium-class))
    (create-webica-core src-webica-core generate-webica-core)))

(defn update-code
  "Will (re) generate Clojure source code in src/webica/ for
the Selenium version **selenium** (or the latest if not specified).

NOTE: the build.boot will also be updated with the given version of Selenium.

If **dry-run** is true the code will not actually be updated."
  {:added "clj0"}
  [dry-run selenium]
  (let [build-boot (fs/file "." "build.boot")]
    (if-not (fs/exists? build-boot)
      (throw (AssertionError.
               (str "Not in webica source tree, cannot find: " build-boot)))
      (let [versions (get-selenium-versions)
            selenium (or selenium (last versions))]
        (if-not (some #(= % selenium) versions)
          (throw (AssertionError.
                   (str "Invalid Selenium version: " selenium
                     " please choose one of: " versions)))
          (let [build (slurp build-boot)
                match (re-find #"\(def selenium-version \".*\"\)" build)
                selenium-version (if match
                                   (string/replace match
                                     #"\(def selenium-version \"(.*)\"\)" "$1"))]
            (println "current selenium-version:" selenium-version)
            (println "generating for selenium version:" selenium)
            (if dry-run
              (println "dry run only...\nselenum versions:" versions)
              (if (not= selenium selenium-version)
                (let [build-new (string/replace build
                                  #"\(def selenium-version \"(.*)\"\)"
                                  (str "(def selenium-version \""
                                    selenium "\")"))
                      build-boot-prev (fs/file "." "build.boot.prev")
                      cmd ["boot" "generate" "--selenium" selenium]]
                  (if (fs/exists? build-boot-prev)
                    (fs/delete build-boot-prev))
                  (fs/rename build-boot build-boot-prev)
                  (spit build-boot build-new)
                  (println "updating build.boot..."
                    (apply str (interpose " " cmd)))
                  (try
                    (let [{:keys [exit out err]} (apply sh cmd)]
                      (if (zero? exit)
                        (println out
                          "\nwebica updated to selenium version: " selenium)
                        (println err
                          "\nunable to update webica to selenium version: "
                          selenium)))
                    (catch Exception e
                      (println "unable to update build.boot:"
                        (.. e getCause getMessage)))))
                (update-webica)))))))))
