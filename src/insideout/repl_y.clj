(ns insideout.repl-y
  "A repl-y module for InsideOut.  Repl-y has a lot of dependencies but more features
  than stock nrepl."
  (:require [insideout.dynamo :as dyn]))

(dyn/require-libs
 [['reply "0.5.1"]]
 ['reply.main])

(def default-opts {:custom-eval '(do (println "REPL-y launching..."))
                   :port 9999})

(defn background-repl-y!
  "Start repl-y in a background thread."
  [opts]
  (let [t (Thread.
           (fn [] ((ns-resolve 'reply.main 'launch) (or opts default-opts))))]
    (.start t)))


(defn start!
  "Start repl-y using the current thread."
  [opts]
  ((ns-resolve 'reply.main 'launch) (or opts default-opts)))
