(ns startrek.infer-types
  (:use clojure.test)
  (:require [clojure.core.typed :as t]
            [clojure.core.typed.runtime-infer :as infer]))

(t/install #{:load})

(defn delete-anns [nss]
  (doseq [ns nss]
    (infer/delete-generated-annotations
      ns
      {:ns ns})))

(defn infer-anns [nss]
  (doseq [ns nss]
    (t/runtime-infer :ns ns)))

(def infer-files
  '[startrek.core
    ])

;; FIXME shouldn't need this, but some types
;; don't compile
(delete-anns infer-files)

(def tests 
  '[startrek.core-test
    startrek.nav-test
    startrek.world-test
    startrek.klingon-test
    startrek.enterprise-test])


;; FIXME need to forcibly reload the :lang'd file. Why?
(apply require tests)
(apply run-tests tests)

(defn play-the-game []
  (let [rdr 
        (clojure.lang.LineNumberingPushbackReader.
          (java.io.StringReader.
            (str (apply str
                        (interpose
                          "\n"
                          (cons 1
                                (repeatedly 8 #(rand-int 3)))))
                 ;; might invoke a NPE, but it usually
                 ;; guarantees the game ends.
                 "q\n")))]
    (binding [*in* rdr]
      (try
        (startrek.core/-main)
        (catch NullPointerException _)))))

;(play-the-game)

(infer-anns infer-files)
