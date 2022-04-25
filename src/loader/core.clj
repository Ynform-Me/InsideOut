(ns loader.core
  "InsideOut program launcher."
  (:require
   [clojure.java.io  :as io]
   [clojure.string   :as str]
   [insideout.dynamo :as dyn])
  (:import
   [java.io File FileNotFoundException])
  (:gen-class :main true))


(def ^{:doc "A symbol naming the default user main namespace"}
  user-main-fn 'insideout.user/-main)

(defn ask-user
  "Prompt user for input until validate returns falsey."
  [prompt-string validate]
  (loop []
    (print (str prompt-string " "))
    (flush)
    (let [maybe-result (str/trim (read-line))]
      (if (validate maybe-result)
        maybe-result
        (recur)))))

(comment
  (ask-user "Create default project layout? (y/n)"
            #{"y" "Y" "n" "N"})
  )

(defn default-main
  "A fallback main function that offers to create a default project structure and exits with a nonzero code."
  [& args]
  (let [create-project (str/lower-case
                        (ask-user "Clojure project not found.  Create default project layout? (y/n)"
                                  #{"y" "Y" "n" "N"}))]
    (if (= create-project "y")
      (let [readme    (slurp (io/resource "README.md"))
            userclj   (slurp (io/resource "user.clj.txt"))
            gitignore (slurp (io/resource "gitignore.txt"))]
        (-> (File. "src/insideout") (.mkdirs))
        (-> (File. "resources") (.mkdirs))

        (spit "README.md" readme)
        (spit "src/insideout/user.clj" userclj)
        (spit ".gitignore" gitignore)

        (println "Created standard project structure.  See README.md for details."))

      (let [search-nss (->> [(first args) user-main-fn]
                            (filter identity)  ; if (first args) is nil remove it
                            (map symbol)
                            (vec))]
        (println (str "\nError: Main function could not be found.\n       Searched in namespaces " search-nss)))))

  (System/exit 1))

(defn maybe-main
  "Tries to resolve namespaces symbol to a function.  If successful, returns
   a vector containing the function and the `args`.  Otherwise returns nil."
  [sym args]
  (when-let [main-ns (namespace sym)]
    (try
      (require [(symbol main-ns)])
      (if-let [main (some-> sym resolve var-get)]
        [main args]
        (println (str "Warning: Namespace found but main function could not be resolved: " [sym])))
      (catch FileNotFoundException e
        nil))))

(defn main-var
  "Find the user's main function or fall-back to 'default-main."
  [args]
  (or (maybe-main (-> (first args) (or "") symbol) (rest args))
      (maybe-main user-main-fn args)
      [default-main args]))


(defn -main
  "public static void main..."
  [& args]
  (dyn/add-source-folders-to-classpath)
  (let [[main args'] (main-var (flatten (vector args)))]
    (apply main args')

    (System/exit 0)))
