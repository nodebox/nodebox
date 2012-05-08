; Clojure string library for testing.
(ns clojure-strings)

(defn str-reverse [s]
  "Reverse a string"
  (apply str (reverse s)))

; The last statement in the file is a var containing a vector with maps for each node.
; The name of the var is not important, but it *is* important that it is part of the namespace.
(def nodes [{:name "reverse" :fn str-reverse}])
