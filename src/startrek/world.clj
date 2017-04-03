(ns startrek.world
   (:require [startrek.utils :as u :refer :all])
   (:require [startrek.enterprise :as e :refer :all])
   (:require [clojure.math.numeric-tower :as math]))

;; populating quadrants
(declare assign-quadrant-klingons assign-quadrant-starbases assign-quadrant-stars reset-quadrants)

;; placing current quadrant items
(declare assign-sector-item assign-sector-klingon place-quadrant)

;; scanner tech
(declare lrs-row lrs-points create-empty-lrs-history display-scan)

;; misc functions
(declare remaining-klingon-count is-docked? dock)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public commands intended to be exported by this namespace
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
; fetch default game state
(defn reset-game-state!
  "Resets the enterprise and the quadrants."
  [game-state]
  (let [stardate (* 100 (+ 20 (gen-uniform 1 21)))
        quads (reset-quadrants)]

    (reset! game-state
            {:enterprise (reset-enterprise)
             :quads quads
             :current-sector []
             :current-klingons []
             :lrs-history (create-empty-lrs-history)
             :starting-klingons (remaining-klingon-count quads)
             :stardate {:start stardate :current stardate :end 30}})))

(defn new-game-state
  "Create a new world game state. Resets the enterprise and the quadrants."
  []
  (doto (atom {}) reset-game-state!))

(defn short-range-scan-command 
  "Creates most of the visible UI for the player. You get to see
  where the enterprise is in relationship to everything else. This
  makes inter-quadrant navigation possible. Should display current
  quadrant grid and status of shields, power, torpedos, etc." 
  [game-state]
  (with-local-vars [condition 3]
    (cond
      (is-docked? game-state) (dock game-state)
      (pos? (count (get-in @game-state [:current-klingons]))) (var-set condition 2)
      (< 300 (get-in @game-state [:enterprise :energy])) (var-set condition 1)
      :else (var-set condition 0))

    (if (neg? (get-in @game-state [:enterprise :damage :short_range_sensors]))
      (u/message "\n*** SHORT RANGE SENSORS ARE OUT ***\n")
      (display-scan game-state condition))))

(defn long-range-scan-command 
  "Used by the player to find nearby klingons without having to enter every
  quadrant. If the lrs scanners are damaged this command will not work. If
  the computers are damaged, the command will work but the results of the
  command will not be stored into the history.

  SHOWS CONDITIONS IN SPACE FOR ONE QUADRANT ON EACH SIDE
  OF THE ENTERPRISE IN THE MIDDLE OF THE SCAN.  THE SCAN
  IS CODED IN THE FORM XXX, WHERE THE UNITS DIGIT IS THE
  NUMBER OF STARS, THE TENS DIGIT IS THE NUMBER OF STAR-
  BASES, THE HUNDREDS DIGIT IS THE NUMBER OF KLINGONS.
  "
  [game-state]
  (if (neg? (get-in @game-state [:enterprise :damage :long_range_sensors]))
    (u/message "LONG RANGE SENSORS ARE INOPERABLE")
    (do 
      (let [q (get-in @game-state [:enterprise :quadrant])
            scan (lrs-points q)]
        (u/message "LONG RANGE SENSOR SCAN FOR QUADRANT" (u/point-2-str q))

        (u/message "-------------------")
        (u/message "|" (clojure.string/join " | " (lrs-row game-state (first scan))) "|")
        (u/message "|" (clojure.string/join " | " (lrs-row game-state (second scan))) "|")
        (u/message "|" (clojure.string/join " | " (lrs-row game-state (last scan))) "|")
        (u/message "-------------------")
        ))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; These methods are used to check on the current world state.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn nearby-space 
  "Awesome helper. First the set of all valid coordinates within
  1 step of the current position. Can be used for quadrants or
  sectors."
  [coord]                                                                           
  (filter seq
          (for [x (range (dec (first coord)) (+ 2 (first coord)))
                y (range (dec (second coord)) (+ 2 (second coord)))]
            (if (every? false? (concat (map #(< % 1) [x y])
                                       (map #(> % 8) [x y])))
              [x y]))))

(defn is-docked? 
  "Used to detect if the enterprise is docked with a nearby
  Starbase."
  [game-state]
  (let [sector (get-in @game-state [:enterprise :sector])
        current-sector (get-in @game-state [:current-sector])]
    (->> (nearby-space sector)
         (filter seq)
         (map #(u/coord-to-index %))
         (map #(get current-sector %))
         (map #(= 3 %))
         (some true?))))

(defn remaining-klingon-count
  "Returns the number of klingon ships left in the game."
  [quadrants]
  (reduce + (map #(:klingons %) quadrants)))

(defn remaining-starbase-count
  "Returns the number of starbases remaining in the game."
  [quadrants]
  (reduce + (map #(:bases %) quadrants)))

(defn- create-empty-lrs-history []
  (vec (for [y (range 1 (+ 1 dim)) x (range 1 (+ 1 dim))] "000")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; All of these methods are used for entering a new quadrant
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- dock [game-state]
    (swap! game-state update-in [:enterprise] merge {:energy 3000 :photon_torpedoes 10 :shields 0})
    (u/message "SHIELDS DROPPED FOR DOCKING PURPOSES"))

(defn- status [condition]
  (condp = condition
    1 "YELLOW"
    2 "RED"
    3 "DOCKED"
    "GREEN"))

(defn display-scan [game-state condition]
  (let [display-field
        (->> (get-in @game-state [:current-sector])
             (map #(if  (zero? %)  "   " %))
             (map #(if  (= 1 %)  "<*>" %))
             (map #(if  (= 2 %)  "+++" %))
             (map #(if  (= 3 %)  ">!<" %))
             (map #(if  (= 4 %)  " * " %))
             (partition 8)
             (map #(clojure.string/join "" %))
             (vec))]

    (u/message "-=--=--=--=--=--=--=--=-")
    (u/message (get display-field 0))
    (u/message (format "%s STARDATE  %d" (get display-field 1) (get-in @game-state [:stardate :current])))
    (u/message (format "%s CONDITION %s" (get display-field 2) (status condition)))
    (u/message (format "%s QUADRANT  %s" 
                       (get display-field 3) 
                       (u/point-2-str (get-in @game-state [:enterprise :quadrant]))))
    (u/message (format "%s SECTOR    %s" 
                       (get display-field 4) 
                       (u/point-2-str (get-in @game-state [:enterprise :sector]))))
    (u/message (format "%s ENERGY    %d" 
                       (get display-field 5) 
                       (int (get-in @game-state [:enterprise :energy]))))
    (u/message (format "%s SHIELDS   %d" 
                       (get display-field 6) 
                       (int (get-in @game-state [:enterprise :shields]))))
    (u/message (format "%s PHOTOTN TORPEDOS %d" 
                       (get display-field 7) 
                       (get-in @game-state [:enterprise :photon_torpedoes])))
    (u/message "-=--=--=--=--=--=--=--=-")))

(defn- lrs-points [quadrant]
  (let [xmin (dec (first quadrant)) 
        xmax (+ 2 (first quadrant))
        ymin (dec (second quadrant))
        ymax (+ 2 (second quadrant))
        valid-idx (apply sorted-set (range 1 (inc dim)))
        ]
    (for [y (range ymin ymax)]
      (vec
        (for [x (range xmin xmax)]
          (if (and (contains? valid-idx x) (contains? valid-idx y))
            [x y]
            []))))))

(defn update-lrs-cell [game-state quad]
  (swap! game-state assoc-in [:lrs-history (u/coord-to-index (:quadrant quad))] 
         (format "%d%d%d" (:bases quad) (:klingons quad) (:stars quad))))

(defn- lrs-row [game-state row]
  (->> row
       (map #(if (empty? %) -1 (u/coord-to-index %)))
       (map #(if (< % 0) {} (get-in @game-state [:quads %])))
       (map #(if (empty? %) 
               "000" 
               (do 
                 (update-lrs-cell game-state %)
                 (get-in @game-state [:lrs-history (u/coord-to-index (:quadrant %))]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; All of these functions are used to fill quadrants with actors.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- create-quadrants []
  (vec (for [y (range 1 (inc dim)) x (range 1 (inc dim))]
         {:quadrant [x y]
          :klingons (assign-quadrant-klingons) 
          :stars (assign-quadrant-stars)
          :bases (assign-quadrant-starbases)})))

(defn- reset-quadrants
  "Returns a new quadrants 2d-array that is guaranteed to have at least 1 klingon and 1 starbase."
  []
  (loop []
    (let [quadrants (create-quadrants)]
      (cond
        (<= (remaining-klingon-count quadrants) 0) (recur)
        (<= (remaining-starbase-count quadrants) 0) (recur)
        :else quadrants))))

(defn- assign-quadrant-klingons
 "Returns 0-3 klingons in a quadrant based on a uniform distribution."
 []
  (let [chance (gen-double)]
    (condp < chance
      0.98 3
      0.95 2
      0.8  1
      0)))

(defn- assign-quadrant-starbases
 "Returns 0-1 starbases in a quadrant based on a uniform distribution."
  []
  (let [chance (gen-double)]
    (condp < chance
      0.96 1
      0)))

(defn- assign-quadrant-stars
  "Returns 1-9 stars for each sector based on a uniform distribution."
  [] 
  (gen-uniform 1 9))

(defn- create-empty-quadrant []
  (vec (for [y (range 1 (+ 1 dim)) x (range 1 (+ 1 dim))] 0)))

(defn place-quadrant
  "Initialize a quadrant with the right amount of game related items."
  [game-state]

  (let [quad (get (:quads @game-state)
                  (coord-to-index (get-in @game-state [:enterprise :quadrant])))
        sector (atom (create-empty-quadrant))]
    (swap! sector assoc 
           (coord-to-index (get-in @game-state [:enterprise :sector]))
           enterprise-id)

    (loop [k (:klingons quad)]
      (when (pos? k)
        (assign-sector-klingon sector klingon-id game-state)
        (recur (dec k))))
    (loop [k (:bases quad)]
      (when (pos? k)
        (assign-sector-item sector base-id)
        (recur (dec k))))
    (loop [k (:stars quad)]
      (when (pos? k)
        (assign-sector-item sector star-id)
        (recur (dec k))))
    (swap! game-state assoc-in [:current-sector] @sector)))

(defn- assign-sector-item
  "Assign an item in a sector to a location."
  [sector value]
  (loop []
    (let [x (gen-idx) y (gen-idx)]
      (if (pos? (get @sector (coord-to-index [x y])))
        (recur)
        (swap! sector assoc (coord-to-index [x y]) value) ))))

(defn- assign-sector-klingon
  "Assign an item in a sector to a location."
  [sector value game-state]
  (loop []
    (let [x (gen-idx) y (gen-idx)]
      (if (pos? (get @sector (coord-to-index [x y])))
        (recur)
        (do
          (swap! game-state
                 assoc-in [:current-klingons]
                          (conj (get-in @game-state [:current-klingons]) {:sector [x y] :energy 200}))
          (swap! sector assoc (coord-to-index [x y]) value))))))

