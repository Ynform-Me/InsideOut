(ns loader.classpath
  (:require [clojure.java.io file])
  (:import [java.io File]
           [clojure.lang DynamicClassLoader]))


(defn dyn-classloader
  "Return the dynamic classloader for the thread's context classloader.  If a dynamic
   classloader hasn't been added to the current thread, one (that delegates to the thread's
   current classloader) is created and registered."
  (^DynamicClassLoader []
   (dyn-classloader (Thread/currentThread)))

  (^DynamicClassLoader [thread]
   (let [cl (-> thread .getContextClassLoader)]
     (if (instance? DynamicClassLoader cl)
       cl
       (let [dcl (DynamicClassLoader. cl)]
         (-> thread (.setContextClassloader dcl)))))))


(defn new-thread
  "Return a new thread preconfigured with the InsideOut classloader.  Doesn't call `start`."
  (^Thread []
   (let [t (Thread.)]
     (.setContextClassloader t (dyn-classloader))
     t))
  (^Thread [^Runnable runnable]
   (let [t (Thread. runnable)]
     (.setContextClassloader t (dyn-classloader))
     t)))


(defn add-urls-to-classpath
  "Adds the specified URLs to the classpath."
  [urls]
  (let [cl ^DynamicClassLoader (dyn-classloader)]
    (doseq [u urls]
      (.addURL cl u))))


(defn find-src+test+res
  ([]
   (find-src+test+res (File. ".")))
  ([root-path]
   (let [conv-over-config [["src/main/clojure" "src/clojure" "src/main" "src"]
                           ["src/main/resources" "src/resources" "resources"]
                           ["src/test/clojure" "test/clojure" "src/test" "test"]]]
     (mapcat
      (fn [paths]
        (take 1 (filter #(->> (File. root-path %) (.exists)) paths)))
      conv-over-config))))


;(def out-of-date-namespaces (nt/ns-tracker (find-src+test+res)))

(defn add-source-folders-to-classpath
  "Adds the java.io.File objects in *classpath-dirs* to the classpath."
  [root-path]
  (let [root-path-iofile (if (instance? File root-path) root-path (file root-path))])
  (let [project-src-folders (map (fn [rel-path] (-> rel-path File. .toURL))
             (find-src+test+res))])
  (add-urls-to-classpath *classpath-dirs*))


