(ns startrek.infer-startrek
  (:use clojure.test)
  (:require [clojure.core.typed :as t]
            [clojure.core.typed.runtime-infer :as infer])
  (:import (java.util.concurrent TimeoutException FutureTask TimeUnit)))

(t/install #{:load})

(def ^:dynamic *infer-fn* t/runtime-infer)

(defn delete-anns [nss]
  (doseq [ns nss]
    (infer/delete-generated-annotations
      ns
      {:ns ns})))

(defn infer-anns [nss]
  (doseq [ns nss]
    (*infer-fn* :ns ns)))

(def infer-files
  '[startrek.core
    ])

(defn play-the-game []
  (let [rdr 
        (clojure.lang.LineNumberingPushbackReader.
          (java.io.StringReader.
            (str (apply str
                        (interpose
                          "\n"
                          [1 1 1 2 4 2 5 23 "q"]))
                 )))]
    (binding [*in* rdr]
      (try
        ((find-var 'startrek.core/-main))
        (catch NullPointerException _)))))

(def tests 
  '[startrek.core-test
    startrek.nav-test
    startrek.world-test
    startrek.klingon-test
    startrek.enterprise-test])

(defn exercise-tests []
  ;; FIXME need to forcibly reload the :lang'd file. Why?
  (apply require tests)
  (apply run-tests tests)

  (play-the-game)
  )

(defn infer [spec-or-type]
  (binding [*infer-fn* (case spec-or-type
                         :type t/runtime-infer
                         :spec t/spec-infer)]


    ;; FIXME shouldn't need this, but some types
    ;; don't compile
    (delete-anns infer-files)

    (exercise-tests)

    (infer-anns infer-files)))
