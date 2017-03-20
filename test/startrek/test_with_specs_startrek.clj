(ns startrek.test-with-specs-startrek
  (:require [startrek.core :as c]
            [clojure.test :as test]
            [startrek.infer-startrek :as inf]
            [clojure.spec.test :as stest]))

(defn activate-specs []
	(stest/instrument 
		(filter (comp #{'startrek.core} symbol namespace)
						(stest/instrumentable-syms))))

(println "Activated specs:\n" (activate-specs))
(println "To prove specs are actually enabled, here is a bad call to (game-over? nil)")
(println
  (try (c/game-over? nil)
       (catch Throwable e e)))

(prn "The above lines should show a spec error from a bad call to (game-over? nil)")

(inf/exercise-tests)
