(ns insideout.foundation
  "The smallest reasonable subset of the clj_foundation library to include in InsideOut's runtime."
  (:require
   [clojure.java.io    :as io]
   [clojure.string     :as str]
   [clojure.stacktrace :as st]))


;; File paths and stuff ------------------------------------------------------------------------------

(defn expand-path
  "Expand ~ in path to the value of the 'user.home' system property.
  FIXME: Not portable to Windows."
  [p]
  (let [home (System/getProperty "user.home")]
    (if (str/includes? p "~")
      (str/replace p "~" home)
      p)))

(defn full-path
  "Resolve a relative path to its corresponding full/canonical path."
  [^String relative-path] (.getCanonicalPath (io/file relative-path)))

(defn file-details
  "Given a java.io.File or a String pointing to a valid file in the file system,
   returns a map with detailed information on the corresponding file."
  [fileOrName]
  (letfn [(extension [f]
            (let [name (.getName f)
                  last-dot (.lastIndexOf name ".")]
              (if (and (not= last-dot -1) (not= last-dot -))
                (.substring name (+ last-dot 1))
                "")))]

    (let [f (if (string? fileOrName) (io/file fileOrName) fileOrName)]
      {:full-name (.getCanonicalPath f)
       :short-name (.getName f)
       :extension (extension f)
       :directory (.isDirectory f)
       :hidden (.isHidden f)
       :last-modified-millis (.lastModified f)})))

;; Retain intermediate steps in a map -----------------------------------------------------------------------

(defmacro let-map
  "A version of let that returns its local variables in a map.
  If a result is computed in the body, and that result is another map,
  let-map returns the result of conj-ing the result map into the let
  expression map.  Otherwise it returns a vector containing the let
  expression  map followed by the result."
  [var-exprs & body]
  (let [vars (map (fn [[var form]] [(keyword var) var]) (partition 2 var-exprs))
        has-body (not (empty? body))]
    `(let [~@var-exprs
           result# (do ~@body)
           mapvars# (into {} [~@vars])]
       (if ~has-body
         (if (map? result#)
           (conj mapvars# result#)
           [mapvars# result#])
         mapvars#))))


(defmacro letfn-map
  "A version of letfn that returns its functions in a map.
  If a result is computed in the body, and that result is another map,
  fn-map returns the result of conj-ing the result map into the function
  map.  Otherwise it returns a vector containing the function map
  followed by the result."
  [fn-exprs & body]
  (let [fn-refs (map (fn [f] [(keyword (first f)) (first f)]) fn-exprs)
        has-body (not (empty? body))]
    `(letfn [~@fn-exprs]
       (let [result# (do ~@body)
             mapfns# (into {} [~@fn-refs])]
         (if ~has-body
           (if (map? result#)
             (conj mapfns# result#)
             [mapfns# result#])
           mapfns#)))))


;; Exceptions and stack traces ----------------------------------------------------------

(def ^:dynamic ^{:doc "The barf/vomit stream.  Defaults to *out*"}
  *vomit-stream* *out*)

(defn -vomit [context t]
  (binding [*out* *vomit-stream*]
    (let [err (ex-info "Unexpected error.  Vomiting!:" {:context context} t)]
      (st/print-stack-trace err)
      (st/print-cause-trace err))))

(defmacro maybe-vomit
  "A guard for code that has to keep executing / stay alive even if an exception was
  `throw`n up the stack.  Why vomit?  Because `slurp` and `spit` were feeling lonely.

  Pass any `context` you want captured in the barf stream.

  Executes `forms` inside a try/catch.

  If an exception occurs, creates an `ex-info` containing `context` that wraps the exception.  Then
  barfs the full `ex-info` stack trace to `*barf-stream*` (which is bound to `*out*` by default).

  Then returns nil."
  [context & forms]
  `(try
     ~@forms
     (catch Throwable t#
       (-vomit ~context t#)
       nil)))


;; Retain intermediate steps in a map -----------------------------------------------------------------------

(defmacro let-map
  "A version of let that returns its local variables in a map.
  If a result is computed in the body, and that result is another map,
  let-map returns the result of conj-ing the result map into the let
  expression map.  Otherwise it returns a vector containing the let
  expression  map followed by the result."
  [var-exprs & body]
  (let [vars (map (fn [[var form]] [(keyword var) var]) (partition 2 var-exprs))
        has-body (not (empty? body))]
    `(let [~@var-exprs
           result# (do ~@body)
           mapvars# (into {} [~@vars])]
       (if ~has-body
         (if (map? result#)
           (conj mapvars# result#)
           [mapvars# result#])
         mapvars#))))


(defmacro letfn-map
  "A version of letfn that returns its functions in a map.
  If a result is computed in the body, and that result is another map,
  fn-map returns the result of conj-ing the result map into the function
  map.  Otherwise it returns a vector containing the function map
  followed by the result."
  [fn-exprs & body]
  (let [fn-refs (map (fn [f] [(keyword (first f)) (first f)]) fn-exprs)
        has-body (not (empty? body))]
    `(letfn [~@fn-exprs]
       (let [result# (do ~@body)
             mapfns# (into {} [~@fn-refs])]
         (if ~has-body
           (if (map? result#)
             (conj mapfns# result#)
             [mapfns# result#])
           mapfns#)))))

;; Object orientation implemented on top of let-map and letfn-map -------------------------------------------


(defn- find-member [o method-key]
  (let [root-value (get o method-key)]
    (if root-value
      root-value
      (when-let [_m (:methods o)]
        (find-member _m method-key))
      (when-let [_clazzs (:parent-classes o)]
        (if (seq? _clazzs)
          (doseq [_clazz _clazzs]
            (find-member _clazz method-key))
          (find-member _clazzs method-key))))))


(defn =>
  "Immutable Javascript-inspired objects.  Mainly because sometimes overriding \"methods\" helps testability.

   * => works the same way for retrieving both property values and invoking methods.  (Uniform access principle.)
   * Uses existing let-map and letfn-map for constructing objects.

   A few examples:

   ```
   (def p (let-map
    [first-name \"Fred\"
     last-name  \"Flintstone\"
     methods   (letfn-map
      [(full-name [] (str first-name \" \" last-name)])]))

   (=> p :first-name)  ; => \"Fred\"
   (=> p :full-name)   ; => \"Fred Flinstone\"
   ```

   A class just needs a constructor function that builds the thing it's named after.

   ```
   (defn Person [first-name last-name]
    (let-map
     [first-name first-name
      last-name  last-name
      methods    (letfn-map
       [(full-name [this] (str first-name \" \" last-name)])]))
   ```

   Extend a class by providing one or more :parent-classes.

   ```
   (defn Flintstone [first-name last-name]
    (let-map
     [pet \"Dino\"
      parent-classes (Person first-name last-name)]))
   ```"
  [object-map method-key & arguments]
  (let [args (concat [object-map] (if arguments arguments []))
        f    (find-member object-map method-key)]
    (if (and f (fn? f))
      (apply f args)
      (if-not (nil? f)
        f
        (throw (IllegalArgumentException. (str method-key " is undefined")))))))
