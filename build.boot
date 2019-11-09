(def project 'webica)
(def description "A Clojure binding for Selenium WebDriver")
(def project-url "https://github.com/tmarble/webica")
(def selenium-version "3.8.1")
(def webica-version "-clj0")
(def version (str selenium-version webica-version))

(set-env!
  :source-paths #{"src" "test"}
  :dependencies (conj
                  '[[org.clojure/clojure "1.9.0" :scope "provided"]
                    [camel-snake-kebab "0.4.0"]
                    [environ "1.1.0"]
                    [me.raynes/fs "1.4.6"]
                    [avenir "0.2.2"]
                    [com.taoensso/timbre "4.10.0"]
                    [pom-versions "0.1.2"]
                    ;; testing/development ONLY
                    [adzerk/boot-test "1.2.0" :scope "test"]
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
  (set-env! :source-paths #{"generate"})
  (update-code :dry-run dry-run :selenium selenium))

;; For Emacs if you Customize your Cider Boot Parameters to 'cider-boot'
;; then this task will be invoked upon M-x cider-jack-in
;; which is on C-c M-j
;; https://cider.readthedocs.io/en/latest/up_and_running/

;; These tasks is conditional on having cider defined which typically
;; is done in ~/.boot/profile.boot
;;
;; FFI see also: https://github.com/tmarble/tenzing3
;; Uncomment below if you use CIDER

;; (deftask clj-dev
;;   "Clojure REPL for CIDER"
;;   []
;;   (comp
;;     (cider)
;;     (repl :server true)
;;     (wait)))

;; (deftask cider-boot
;;   "Cider boot params task"
;;   []
;;   (clj-dev))
