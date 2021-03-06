(ns basic-combat-ai.systems
  (:import [com.badlogic.gdx.graphics Texture OrthographicCamera] 
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.components :as comps]
            [basic-combat-ai.entities :as ents-ns]
            [basic-combat-ai.behavior-tree :as bt]
            [basic-combat-ai.math-utils :as math-utils]
            [basic-combat-ai.tile-map :as tile-map]))

(defn render [{{ents :entities} :ecs batch :batch cam :camera}]
  (let [qualifying-ents (filterv #(and (:renderable %) (:transform %)) ents)]
    (.setProjectionMatrix batch (.combined cam))
    (.begin batch)
    ;this loop isn't really any faster than using map. the slow part is the actual drawing done by libgdx/opengl
    (loop [q-ents qualifying-ents]
      (if (empty? q-ents)
        ents
        (let [e (first q-ents)
              texture-region (:texture (:renderable e))
              x (float (get-in e [:transform :x]))
              y (float (get-in e [:transform :y]))
              origin-x (float (get-in e [:transform :origin-x]))
              origin-y (float (get-in e [:transform :origin-y]))
              ;libgdx draws rotation counter clock wise, and um, i want to keep my code clock wise because it  makes more sense to me.
              rotation (* -1.0 (float (get-in e [:transform :rotation]))) 
              width (float (.getRegionWidth texture-region))
              height (float (.getRegionHeight texture-region))
              scale-x (float (:scale-x (:renderable e)))
              scale-y (float(:scale-y (:renderable e)))]
          (.draw batch texture-region x y origin-x origin-y width height scale-x scale-y rotation)
          (recur (rest q-ents)))))
    (.end batch))
  ents)

(defn- set-current-frame-num [entity cf-num]
  (assoc-in entity [:animation :current-frame] cf-num))

(defn- set-renderable [entity frame-num frames]
  (comps/renderable entity (:texture (nth frames frame-num))))

(defn- animate* [e delta]
  (if-not (get-in e [:animation :current-animation])
	  e
	  (let [current-duration (get-in e [:animation :current-duration])
	        current-frame-num (get-in e [:animation :current-frame])
	        current-animation (get-in e [:animation :current-animation])
	        current-frames (get-in e [:animation :frames current-animation :frame-duration])]
	    (cond
        (neg? current-frame-num)
        (-> e 
          (set-current-frame-num 0)
          (assoc-in [:animation :current-duration] (:duration (first current-frames)))
          (set-renderable 0 current-frames))
	          
	      (> current-duration 0)
	      (assoc-in e [:animation :current-duration] (- current-duration delta))
	    
	      (and (neg? current-duration) (< current-frame-num (dec (count current-frames))))
	      (-> e 
	         (set-current-frame-num (inc current-frame-num))
	         (set-renderable (inc current-frame-num) current-frames)
	         (assoc-in [:animation :current-duration] (:duration (nth current-frames (inc current-frame-num)))))
	      
	      (= current-frame-num (dec (count current-frames)))
	      (if (get-in e [:animation :frames current-animation :loop?])
		      (-> e 
		        (set-current-frame-num 0)
            (set-renderable 0 current-frames)
		        (assoc-in [:animation :current-duration] (:duration (first current-frames))))
		      (-> e
		        (assoc-in [:animation :current-animation] nil)
		        (set-current-frame-num -1)))))))
  
(defn animate [{{ents :entities} :ecs delta :delta :as game}]
  "The current-animation key should be set from somewhere else, like an AI logic system or something."
     (let [qualifying-ents (filterv #(and (:animation %) (:renderable %)) ents)
           rest-ents (filterv #(not (and (:animation %) (:renderable %))) ents)
           modified-ents (mapv #(animate* % delta) qualifying-ents)]  
       (into modified-ents rest-ents)))

(defn- ent-vec->map [entities]
  (loop [ents entities
         ents-map {}]
    (if (empty? ents)
      ents-map
      (let [e (first ents)]
        (recur (rest ents) (assoc ents-map (keyword (str (:id e))) e))))))

(defn tick-behavior-tree [{{ents :entities} :ecs tile-map :tile-map :as game}]
     (let [qualifying-ents (filter #(:behavior-tree %) ents)
           ents-map (ent-vec->map ents)]
       (loop [q-ents qualifying-ents ;only use this for the id's. 
              ents-m ents-map
              t-map tile-map]
         (if (empty? q-ents)
           (vec (vals ents-m))
           ;only using q-ents for the id's. try to do all other read/write to the ent-map, so I don't get confused later and try updating anything in q-ents.
           (let [ent-id (keyword (str (:id (first q-ents))))
                 ;returns {:node (the root) :entities (as map) :tile-map}
                 unchecked-bt (:tree (:behavior-tree (ent-id ents-m)))
                 checked-bt (if (or (= :success (:status unchecked-bt))
                                    (= :failure (:status unchecked-bt)))
                              (bt/reset-behavior-tree unchecked-bt)
                              unchecked-bt)
                 tick-data (if (= (:status checked-bt) :running)
                             (let [id-existing (bt/get-id-of-running-composite checked-bt)
                                   canceled-tick-data  (let [canceled-return-data (bt/cancel-running-leaf checked-bt ent-id ents-m t-map)
                                                             reseted-tree (bt/reset-behavior-tree checked-bt)]
                                                        (bt/tick reseted-tree ent-id (:entities canceled-return-data) (:tile-map canceled-return-data)))
                                   id-canceled (bt/get-id-of-running-composite (:node canceled-tick-data))]
                               (if (or (= (:status (:node canceled-tick-data)) :failure)
                                       (= id-existing id-canceled))
                                 (bt/tick checked-bt ent-id ents-m t-map)
                                 canceled-tick-data))
                             ;status is fresh
                             (bt/tick checked-bt ent-id ents-m t-map))
                 updated-ents-m (:entities tick-data)
                 updated-ent-with-ticked-node (assoc-in (ent-id updated-ents-m) [:behavior-tree :tree] (:node tick-data))]
             ;somewhere i need to do a dirty update to the tile map, directly. yay global variables.
             (recur (rest q-ents) (assoc updated-ents-m ent-id updated-ent-with-ticked-node) (:tile-map tick-data)))))))

(defn- filter-ents [ents pred]
  "returns a vector of vectors. the first vector contains entities that satisfied the predicate. the second vector has entities that did not satisfy the predicate."
  [(filterv pred ents) (filterv #(not (pred %)) ents)])

(defn rotate [{{ents :entities} :ecs}]
	(let [[qualifying-ents rest-ents] (filter-ents ents #(and (:target-rotation %) 
	                                                          (:movespeed %)
	                                                          (:transform %)))
	      modified-ents (mapv (fn [q-ent]
	                            (let [rot (:rotation (:transform q-ent))
															     target-rot (:target-rotation q-ent)
															     rot-speed (:rotation-speed (:movespeed q-ent))
                                   angle-diff (- target-rot rot)
                                   raw-rotation-left (if (> (Math/abs angle-diff) 180)
                                                       (if (neg? angle-diff)
                                                         (+ 360 angle-diff)
                                                         (- angle-diff 360))
                                                       angle-diff)
                                   ;'overflow' check
                                   rotation-increment (if (<= (Math/abs raw-rotation-left) rot-speed)
                                                        raw-rotation-left
                                                        (* (if (neg? raw-rotation-left) -1 1) rot-speed))
															     updated-rotation (-> (+ rot rotation-increment)
                                                      (math-utils/bind-0-359)
                                                      (math-utils/round-to-decimal 1))
                                   rotated-ent (assoc-in q-ent [:transform :rotation] updated-rotation)]
	                             (if  (== target-rot updated-rotation)
                                (dissoc rotated-ent :target-rotation)
                                rotated-ent)))
	                          qualifying-ents)]
	  (into modified-ents rest-ents)))

(defn move [{{ents :entities} :ecs}]
  (let [[qualifying-ents rest-ents] (filter-ents ents #(and (:target-location %) (:movespeed %) (:transform %)))
        degrees->vector (fn [rotation]
                          (let [x (math-utils/round-to-decimal (Math/sin (Math/toRadians rotation)) 1)
                                y (math-utils/round-to-decimal (Math/cos (Math/toRadians rotation)) 1)]
                            [x y]))
        update-ent (fn [e]
                     (let [dir-vec (degrees->vector (:rotation (:transform e)))
                           x-speed (* (nth dir-vec 0) (:movespeed (:movespeed e)))
                           y-speed (* (nth dir-vec 1) (:movespeed (:movespeed e)))
                           tar-loc-x (tile-map/grid->world-coord (:x (:target-location e)))
                           x-inc (if (< (Math/abs (- tar-loc-x (:x (:transform e)))) (Math/abs x-speed))
                                   (- tar-loc-x (:x (:transform e)))
                                   x-speed)
                           tar-loc-y (tile-map/grid->world-coord (:y (:target-location e)))
                           y-inc (if (< (Math/abs (- tar-loc-y (:y (:transform e)))) (Math/abs y-speed))
                                   (- tar-loc-y (:y (:transform e)))
                                   y-speed)]
                       ;if the x and y transforms are equal to the x and y in the target location, then remove the target-location component from the entity and return it.
                       ;else, apply the x increment and the y increment to the transform and return the entity.
                       (if (and (== tar-loc-x (:x (:transform e))) (== tar-loc-y (:y (:transform e))))
                         (dissoc e :target-location)
                         (-> e
                           (update-in [:transform :x] (fn [x] (math-utils/round-to-decimal (+ x x-inc) 1)))
                           (update-in [:transform :y] (fn [y] (math-utils/round-to-decimal (+ y y-inc) 1)))))))
        modified-ents (mapv update-ent qualifying-ents)]
    (into modified-ents rest-ents)))

(defn death [{{ents :entities} :ecs}]
  (let [[qualifying-ents rest-ents] (filter-ents ents #(:hit-points %))
        nil-if-dead (fn [e] (if (<= (:hit-points e) 0) nil e))
        remove-nils (fn [ents] (filter #(not (nil? %)) ents))]
    (-> (map nil-if-dead qualifying-ents)
      (remove-nils)
      (into rest-ents))))

(defn projectile-weapon-cooldown [{{ents :entities} :ecs delta :delta}]
  (let [[qualifying-ents rest-ents] (filter-ents ents #(:projectile-weapon %))
        tick-cooldown (fn [e]
                        (if (pos? (:curr-cooldown (:projectile-weapon e)))
                          (update-in e [:projectile-weapon :curr-cooldown] (fn [ccd] (math-utils/round-to-decimal (- ccd delta) 3)))
                          e))
        modified-ents (map tick-cooldown qualifying-ents)]
    (into rest-ents modified-ents)))

(defn melee-weapon-cooldown [{{ents :entities} :ecs delta :delta}]
  (let [[qualifying-ents rest-ents] (filter-ents ents #(:melee-weapon %))
        tick-cooldown (fn [e]
                        (if (pos? (:curr-cooldown (:melee-weapon e)))
                          (update-in e [:melee-weapon :curr-cooldown] (fn [ccd] (math-utils/round-to-decimal (- ccd delta) 3)))
                          e))
        modified-ents (map tick-cooldown qualifying-ents)]
    (into rest-ents modified-ents)))

(defn create-pending-entity [{{entities :entities} :ecs :as game}]
  (loop [idx 0
         ents entities]
    (if (= idx (count ents))
      ents
      (recur (inc idx)
             (if-let [create-entity-fn (:pending-entity (nth ents idx))]
               (assoc ents idx (create-entity-fn game))
               ents)))))

(defn timed-life [{{entities :entities} :ecs, delta :delta}]
  (loop [ents entities
         result-ents []]
    (if (empty? ents)
      result-ents
      (let [e (first ents)
            timed-life (:timed-life e)]
        (cond
          (nil? timed-life)
            (recur (rest ents) (conj result-ents e))
          (<= timed-life 0) 
            (recur (rest ents) result-ents)
          :else
            (recur (rest ents) (conj result-ents (assoc e :timed-life (- timed-life delta)))))))))

(defn cover [{{entities :entities} :ecs tile-map :tile-map}]
 ;remove in-cover
 ;check if any entities with hp share a tile-grid with current sandbag, other than the sandbag itself.
 ;if one does, give it in-cover with appropriate miss chance.
 (let [removed-in-cover (mapv (fn [e] (dissoc e :chance-to-miss)) entities)
       [cover-ents rest-ents] (filter-ents removed-in-cover #(:provides-chance-to-miss %))
       in-same-tile? (fn [e1 e2]
                       (let [e1-transform (:transform e1)
                             e1-tile-loc {:x (tile-map/world-coord->grid (:x e1-transform)), :y (tile-map/world-coord->grid (:y e1-transform))}
                             e2-transform (:transform e2)
                             e2-tile-loc {:x (tile-map/world-coord->grid (:x e2-transform)), :y (tile-map/world-coord->grid (:y e2-transform))}]
                         (= e1-tile-loc e2-tile-loc)))
       ents-cover (fn [e cover-spots]
                    "return the entity's cover spot, if it doesnt have one then return nil."
                    (loop [c-spots cover-spots]
                      (if (empty? c-spots)
                        nil
                        (let [cs (first c-spots)]
                          (if (in-same-tile? e cs)
                            cs
                            (recur (rest c-spots)))))))
       applicable-ents-have-cover (loop [ents rest-ents
                                         updated []]
                                    (if (empty? ents)
                                      updated
                                      (let [e (first ents)]
                                        (if-let [e-cover (ents-cover e cover-ents)]
                                          (recur (rest ents)
                                                 (conj updated (comps/chance-to-miss e (:provides-chance-to-miss e-cover))))
                                          (recur (rest ents)
                                                 (conj updated e))))))]
   (into cover-ents applicable-ents-have-cover)))

;debug toolz
;
;
;(require '[basic-combat-ai.main-screen :as ms])
;(def prev-games [])
;(defn run-game [game systems]
;  (assoc-in game [:ecs :entities] (loop [syss systems
;                                         ents (get-in game [:ecs :entities])]
;                                    (if (empty? syss)
;                                      ents
;                                      (recur (rest syss) ((first syss) (assoc-in game [:ecs :entities] ents)))))))
;
;(defn run-once [] 
;  (do
;    (if (= 9 (count prev-games)) 
;      (def prev-games (conj (rest prev-games) ms/game))
;      (def prev-games (conj prev-games ms/game)))
;    (ms/update-game! #(assoc-in %
;                                [:ecs :entities]
;                                (get-in (run-game % [tick-behavior-tree
;                                                     create-pending-entity
;                                                     projectile-weapon-cooldown
;                                                     animate
;                                                     rotate
;                                                     move
;                                                     death
;                                                     timed-life])
;                                        [:ecs :entities])))))
;
;(defn run-times [n]
;  (loop [idx n]
;    (if (zero? idx)
;      nil
;      (do
;        (run-once)
;        (recur (dec idx))))))
;
;(defn revert-entities [idx]
;  (if (and (< idx 10) (>= idx 0))
;    (ms/update-game! #(assoc-in % [:ecs :entities] (:entities (:ecs (nth prev-games idx)))))
;    "idx must be between 0 and 9, inclusive."))

;
;(get-in (run-game ms/game [tick-behavior-tree
;                           create-pending-entity
;                           projectile-weapon-cooldown
;                           animate
;                           rotate
;                           move
;                           death
;                           timed-life])
;        [:ecs :entities])

(defn init [game]
  (-> game
    (ecs/add-system render)
    (ecs/add-system tick-behavior-tree)
    (ecs/add-system create-pending-entity)
    (ecs/add-system projectile-weapon-cooldown)
    (ecs/add-system melee-weapon-cooldown)
    (ecs/add-system animate)
    (ecs/add-system rotate)
    (ecs/add-system move)
    (ecs/add-system death)
    (ecs/add-system timed-life)
    (ecs/add-system cover)
    ))