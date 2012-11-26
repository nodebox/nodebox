; Clojure string library for testing.
(ns clojure-strings)

(defn str-reverse [s]
  "Reverse a string"
  (apply str (reverse s)))
