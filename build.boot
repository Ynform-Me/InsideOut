(set-env!
 :dependencies '[[nrepl                  "0.8.3"  :scope "test"]
                 [org.slf4j/slf4j-nop    "1.7.13" :scope "test"]
                 [adzerk/boot-jar2bin    "1.2.0"  :scope "test"]
                 [adzerk/boot-reload     "0.6.1"  :scope "test"]

                 ;; Rich comment form testing (at runtime the `test` macro evaluates to nothingness)
                 [com.hyperfiddle/rcf    "20220405"]

                 ;; Server-side dependencies
                 [org.clojure/clojure    "1.11.0"]
                 [ns-tracker             "0.4.0"]
                 [org.clojure/data.xml   "0.0.8"]
                 [org.slf4j/slf4j-simple "1.7.5"]

                 ;; Git support (TBD)

                 ;; Maven resolver / Pomegranate
                 [org.apache.maven.resolver/maven-resolver-api  "1.6.3"]
                 [org.apache.maven.resolver/maven-resolver-spi  "1.6.3"]
                 [org.apache.maven.resolver/maven-resolver-util "1.6.3"]
                 [org.apache.maven.resolver/maven-resolver-impl "1.6.3"]
                 [org.apache.maven.resolver/maven-resolver-transport-file  "1.6.3"]
                 [org.apache.maven.resolver/maven-resolver-transport-http  "1.6.3"]
                 [org.apache.maven.resolver/maven-resolver-transport-wagon "1.6.3"]
                 [org.apache.maven.resolver/maven-resolver-connector-basic "1.6.3"]
                 ;; https://mvnrepository.com/artifact/org.apache.maven/maven-aether-provider
                 [org.apache.maven/maven-aether-provider        "3.3.9"]
                 [org.apache.maven.wagon/wagon-provider-api     "3.3.2" :exclude [org.codehaus.plexus/plexus-utils]]
                 [org.apache.maven.wagon/wagon-http             "3.3.4"]
                 [org.apache.maven.wagon/wagon-ssh              "3.3.4"]
                 [org.apache.httpcomponents/httpclient          "4.5.8"]
                 [org.apache.httpcomponents/httpcore            "4.4.11"]]

 :repositories {"central"                 "https://repo1.maven.org/maven2/"
                "clojars.org"             "https://repo.clojars.org"
                ;; WIP: P2 repository support
                "artifactory.openntf.org" "https://artifactory.openntf.org/openntf"
                "eclipse.org.p2"          {:url "https://download.eclipse.org/releases/2021-09"
                                           :layout "p2"}}

 :source-paths #{"src"}
 :resource-paths #{"src" "resources"})

(require
 '[boot.pod :as pod]
 '[boot.task.built-in  :refer :all]
 '[adzerk.boot-jar2bin :refer :all]
 '[boot.core :as boot]
 '[clojure.java.io :as io]

 '[adzerk.boot-reload  :refer [reload]])

(task-options!
 pom {:project 'insideout
      :version "0.2.0"})


(deftask ui-scale [m multiplier VAL str "The user interface scale multiplier for Swing and JavaFX"]
  (System/setProperty "sun.java2d.uiScale" multiplier)
  (System/setProperty "glass.gtk.uiScale" multiplier)
  identity)


(deftask cider
  "Add dependencies for development tools that expect Cider/IDE middleware in the REPL."
  []
  (require 'boot.repl)
  (swap! @(resolve 'boot.repl/*default-dependencies*)
         concat '[[cider/cider-nrepl "0.27.3"]
                  [refactor-nrepl "3.5.2"]])
  (swap! @(resolve 'boot.repl/*default-middleware*)
         concat '[cider.nrepl/cider-middleware
                  refactor-nrepl.middleware/wrap-refactor])
  identity)

(deftask reveal
  "Make the repl display a 'reveal' data browser"
  []
  #_(System/setProperty "vlaaad.reveal.prefs" ":theme :dark")
  (require 'boot.repl)
  (swap! @(resolve 'boot.repl/*default-dependencies*)
         concat '[[vlaaad/reveal "1.3.196"]
                  [lambdaisland/deep-diff2 "2.0.108"]])
  (swap! @(resolve 'boot.repl/*default-middleware*)
         concat '[vlaaad.reveal.nrepl/middleware])
  identity)


(deftask dev-mode-settings
  "Enable the user.clj development mode namespace"
  []
  (set-env! :source-paths #{"src" "dev"})
  identity)


(deftask dev
  "Build for local development."
  []
  (comp
   (dev-mode-settings)
   (cider)
   (javac)
   (watch)
   (notify :audible true :visual true :theme "woodblock")
   (repl
    :port 8008
    :server true
    :init-ns 'insideout.core
    :eval '(-main))))

(deftask prod
  "Build for production deployment."
  []
  (comp #_(aot :namespace #{'insideout.boot})
     (javac)
     (pom)
     (uber)
     (jar :main 'loader.Main :file "io.jar")
     (bin :output-dir "bin")))
