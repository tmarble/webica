(def project 'webica)
(def description "A Clojure binding for Selenium WebDriver")
(def project-url "https://github.com/tmarble/webica")
(def selenium-version "3.0.0-beta2")
(def webica-version "-clj0")
(def version (str selenium-version webica-version))

(set-env! :resource-paths #{"src"}
  :source-paths   #{"test"}
  :dependencies   (conj
                    '[[org.clojure/clojure "1.9.0-alpha10" :scope "provided"]
                      [camel-snake-kebab "0.4.0"]
                      [environ "1.1.0"]
                      [me.raynes/fs "1.4.6"]
                      [avenir "0.2.1"]
                      [com.taoensso/timbre "4.7.3"]
                      ;; testing/development ONLY
                      [aleph "0.4.2-alpha6" :scope "test"]
                      [adzerk/boot-test "1.1.2" :scope "test"]
                      [adzerk/bootlaces "0.1.13" :scope "test"]]
                    ['org.seleniumhq.selenium/selenium-java selenium-version]))

(require
  '[adzerk.boot-test :refer [test]]
  '[adzerk.bootlaces :refer :all])

(bootlaces! version)

(task-options!
  test {:namespaces #{'testing.webica.core}}
  pom {:project     project
       :version     version
       :description description
       :url         project-url
       :scm         {:url project-url}
       :license     {"Apache-2.0" "http://opensource.org/licenses/Apache-2.0"}})

(deftask clj-dev
  "Clojure REPL for CIDER"
  []
  (comp
    (cider)
    (repl :server true)
    (wait)))

(deftask cider-boot
  "Cider boot params task"
  []
  (clj-dev))

(deftask local
  "Build jar and install to local repo."
  []
  (comp
    (sift :include #{#"~$"} :invert true) ;; don't include emacs backups
    (pom)
    (jar)
    (install)))

(deftask build-target
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp
      (sift :include #{#"~$"} :invert true) ;; don't include emacs backups
      (pom)
      (jar)
      (target :dir dir))))

(deftask update-code
  "Helper task for generate"
  [n dry-run bool "dry run"
   s selenium VERSION str "selenium version [latest]"]
  (require '[webica.generate])
  (with-post-wrap [_]
    ((resolve 'webica.generate/update-code) dry-run selenium)))

(deftask generate
  "Generate webica Clojure source code from upstream Selenium"
  [n dry-run bool "dry run"
   s selenium VERSION str "selenium version [latest]"]
  (set-env! :resource-paths #{"generate"})
  (update-code :dry-run dry-run :selenium selenium))
