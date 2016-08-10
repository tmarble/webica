# webica

A Clojure binding for [Selenium WebDriver](http://docs.seleniumhq.org/)

[![Clojars Project](https://img.shields.io/clojars/v/webica.svg)](https://clojars.org/webica)

Check out the [CHANGELOG](CHANGELOG.md)

## Background

When testing web applications -- especially in the context of
continuous integration testing -- you need to have a tool
to remotely control a web browser. This is essential for
developing and testing ClojureScript applications and
`*.cljs` or `*.cljc` libraries targeting the browser.

While [clj-webdriver](https://github.com/semperos/clj-webdriver) is
a nice Clojure wrapper for Selenium it often falls behind in tracking
upstream versions and, consequently, often breaks with new browsers
(e.g. when the browser version exceeds the known range).

The purpose of **webica** is to generate a Clojure wrapper for Selenium
making heavy use of introspection in the spirit of
[amazonica](https://github.com/mcohen01/amazonica)
which should track Selenium (the Clojure source code can
be generated nearly automatically) and expose new upstream APIs more quickly.

Because of the use of introspection it is not easy to generate
[codox](https://github.com/weavejester/codox) style API documents
(see [#1](https://github.com/tmarble/webica/issues/1)). However every **webica**
namespace has a `show-functions` function
to explain the symbols available in that namespace.

## Prerequisites

Obviously you need to have a web browser installed. Not
as obviously you also need to have the corresponding driver installed:

* Firefox: [geckodriver](https://github.com/mozilla/geckodriver/releases)
* Chrome: [chromedriver](https://sites.google.com/a/chromium.org/chromedriver/downloads)
* Safari: [safaridriver](https://github.com/SeleniumHQ/selenium/wiki/SafariDriver)
* Edge: [webdriver](https://developer.microsoft.com/en-us/microsoft-edge/tools/webdriver/)

For using Clojure boot scripts (like the example below) or for
developing **webica** please install [boot](http://boot-clj.com/)
if you haven't done so already.

For more on boot see [Sean's blog](http://seancorfield.github.io/blog/2016/02/02/boot-new/) and the [boot Wiki](https://github.com/boot-clj/boot/wiki).
This is how [Clojure boot scripts](https://github.com/boot-clj/boot/wiki/Scripts) work.

## Example

Check out the example Clojure boot script using **webica** that
imitates [Let me Google that for you](lmgtfy.com) on the
command line.

````
./examples/lmgtfy ClojureScript for thought
````

## Development status and Contributing

Please see [CONTRIBUTING](CONTRIBUTING.md) for details on
how to make a contribution.

Note that **webica** does not yet have complete coverage
of all Selenium classes.

Here are some helpful Selenium links
* [Selenium Java API](https://seleniumhq.github.io/selenium/docs/api/java/index.html?org/openqa/selenium/firefox/FirefoxDriver.html)
* [Guava API](https://google.github.io/guava/releases/19.0/api/docs/com/google/common/base/class-use/Function.html) *used by Selenium*
* [Selenium source code](https://github.com/SeleniumHQ/selenium) -- see the Java [CHANGELOG](https://github.com/SeleniumHQ/selenium/blob/master/java/CHANGELOG)

Logging is controlled by the following environment variables:
* **WEBICA_LOG** the name of the log file [webica.log]
* **WEBICA_LEVEL** the desired log level [warn] *must be one of:* `#{"trace" "debug" "info" "warn" "error" "fatal" "report"}`

## Copyright and license

Copyright © 2016 Tom Marble

Licensed under the [Apache License 2.0](http://opensource.org/licenses/Apache-2.0) [LICENSE](LICENSE)
