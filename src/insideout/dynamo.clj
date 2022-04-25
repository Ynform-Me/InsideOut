(ns insideout.dynamo
  "A component/module system for InsideOut built on Pomegranite."
  (:require
   [from.cemerick.pomegranate        :as pom]
   [from.cemerick.pomegranate.aether :as a]
   [loader.classpath                 :as cp])
  (:import
   [clojure.lang DynamicClassLoader]
   [java.io File]))


(def ^:dynamic *extra-repositories*
  "Extra repositories in addition to Maven Central and Clojars. Default=[].
  Future: Use the `eclipse-repo` function to obtain an Eclipse P2 repository configuration
  if you want to add Eclipse/P2 dependencies."
  [])


(defn resolver
  [dependency]
  (let [[[group-archive version]
         specs] [(take 2 dependency)
                 (apply into {} (drop 2 dependency))]]
    ))


(comment
  "WIP"

  (nth [1 2] 2)
  (count [1 2])
  (take 2 (range 1))

  [clojure/spec.alpha "1.1" layout/maven]  ; maven is the default layout
  [clojure/spec.alpha "1.1" repo/github proto/https layout/git]
  [clojure/spec.alpha "1.1" repo/gitlab proto/git]
  [clojure/spec.alpha "1.1" repo/gitlab]  ; implies proto/git
  [clojure/spec.alpha "1.1" proto/git (repo "coconut-palm-software.com/repo/root")]
  [clojure/spec.alpha "1.1" repo/https "coconut-palm-software.com/repo/root"]

  "")


(defn resolve-libs
  "Download and add the specified dependencies to the classpath.  If
  a classloader is specified, use that as the parent classloader else
  use the thread's context classloader.  Default repositories are
  Maven Central and Clojars.  Bind the *extra-repositories* dynamic
  var to add additional repositories beyond these."

  ([classloader coordinates]
   (pom/add-dependencies :classloader classloader
                         :coordinates coordinates
                         :repositories (apply merge from.cemerick.pomegranate.aether/maven-central
                                              {"clojars" "https://clojars.org/repo"}
                                              *extra-repositories*)))
  ([coordinates]
   (resolve-libs (cp/dyn-classloader) coordinates)))


(defn require-libs
  "Download and require namespace(s) directly from Maven-style dependencies.

  [classloader coordinates require-params] or
  [coordinates require-params] where

  classloader - the parent classloader for the new class files
  coordinates - A vector of '[maven.style/coordinates \"1.0.0\"]
  require-params - A vector of parameters to pass to clojure.core/require
                   Or a vector of vectors to sequentially pass to clojure.core/require"

  ([classloader coordinates require-params]
   (resolve-libs classloader coordinates)
   (when-not (empty? require-params)
     (if (every? sequential? require-params)
       (apply require require-params)
       (require require-params))))

  ([coordinates require-params]
   (require-libs (cp/dyn-classloader) coordinates require-params)))


;; This has to be a macro because `import` is a macro and has to
;; be executed inside the namespace into which the class will be imported.
(defmacro import-libs
  "Download and import classes directly from Maven-style dependencies.

  [coordinates import-params] where
  coordinates - A vector of '[maven.style/coordinates \"1.0.0\"]
  import-params - A vector of parameters to pass to clojure.core/import
                  Or a vector of vectors to sequentially pass to clojure.core/import"

  [coordinates import-params]
  (let [imports (if (empty? import-params)
                  []
                  (if (every? sequential? import-params)
                    (map (fn [i] `(import ~i)) import-params)
                    [`(import ~import-params)]))]
    `(do
       (resolve-libs (cp/dyn-classloader) ~coordinates)
       ~@imports)))


(defn classloader-hierarchy
  "Return the current classloader hierarchy."
  []
  (pom/classloader-hierarchy))


(defn get-classpath
  "Return the current classpath."
  []
  (pom/get-classpath))


(comment
  (->> "src" File. .toURL)

  (let [^DynamicClassLoader cl (cp/dyn-classloader)]
    (-> cl (.addURL (->> "src" File. .toURL))))

  ;; Need service classloader management life cycle
  ;;      add deps to service classloader
  ;;      event bus for service comms
  ;;      "atomic" pubsub across services
  ,)
