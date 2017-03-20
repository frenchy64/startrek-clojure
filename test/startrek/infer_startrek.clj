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

;; timing functions from clojail
(def ^{:doc "Create a map of pretty keywords to ugly TimeUnits"}
  uglify-time-unit
  (into {} (for [[enum aliases] {TimeUnit/NANOSECONDS [:ns :nanoseconds]
                                 TimeUnit/MICROSECONDS [:us :microseconds]
                                 TimeUnit/MILLISECONDS [:ms :milliseconds]
                                 TimeUnit/SECONDS [:s :sec :seconds]}
                 alias aliases]
             {alias enum})))


(defn thunk-timeout
  "Takes a function and an amount of time to wait for thse function to finish
  executing. The sandbox can do this for you. unit is any of :ns, :us, :ms,
  or :s which correspond to TimeUnit/NANOSECONDS, MICROSECONDS, MILLISECONDS,
  and SECONDS respectively."
  ([thunk ms]
   (thunk-timeout thunk ms :ms nil)) ; Default to milliseconds, because that's pretty common.
  ([thunk time unit]
   (thunk-timeout thunk time unit nil))
  ([thunk time unit tg]
   (let [task (FutureTask. thunk)
         thr (if tg (Thread. tg task) (Thread. task))]
     (try
       (.start thr)
       (.get task time (or (uglify-time-unit unit) unit))
       (catch TimeoutException e
         (.cancel task true)
         (.stop thr) 
         (throw (TimeoutException. "Execution timed out.")))
       (catch Exception e
         (.cancel task true)
         (.stop thr) 
         (throw e))
       (finally (when tg (.stop tg)))))))


(defn play-the-game []
  (let [rdr 
        (clojure.lang.LineNumberingPushbackReader.
          (java.io.StringReader.
            (str (apply str
                        (interpose
                          "\n"
                          (cons 1
                                (repeatedly 8 #(rand-int 7)))))
                 ;; might invoke a NPE, but it usually
                 ;; guarantees the game ends.
                 "q\n")))]
    (binding [*in* rdr]
      (try
        ((find-var 'startrek.core/-main))
        (catch NullPointerException _)))))

(defn infer [spec-or-type]
  (binding [*infer-fn* (case spec-or-type
                         :type t/runtime-infer
                         :spec t/spec-infer)]


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


    (prn "Playing the game for 20 seconds...")
    (try (thunk-timeout (fn [] (play-the-game)) 20 :s)
         (catch Throwable e e))
    (prn "Finished playing the game for 20 seconds.")

    (infer-anns infer-files)))
