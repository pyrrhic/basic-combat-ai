(ns basic-combat-ai.enemy-ai
  (:require [basic-combat-ai.components :as comps]
            [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.projectile :as projectile]
            [basic-combat-ai.behavior-tree :as bt]
            [basic-combat-ai.tile-map :as tile-map]
            [basic-combat-ai.astar :as astar]
            [basic-combat-ai.math-utils :as math-utils]))

;Queries
(defrecord HasMoveTo [status]
  bt/NodeBehavior
  (bt/reset [node]
    (assoc node :status :fresh))
  (bt/run [node main-ent-id entities tile-map]
    (let [main-ent (main-ent-id entities)]
      (if (:move-to main-ent)
        (bt/make-return-map (assoc node :status :success) entities tile-map)
        (bt/make-return-map (assoc node :status :failure) entities tile-map)))))

(defrecord CombatTargetAlive [status]
  bt/NodeBehavior
  (bt/reset [node]
    (assoc node :status :fresh))
  (bt/run [node main-ent-id entities tile-map]
    (let [main-ent (main-ent-id entities)
          combat-target (get entities (:combat-target-id main-ent))]
      (if (and (not (nil? combat-target)) (pos? (:hit-points combat-target)))
        (bt/make-return-map (assoc node :status :success) entities tile-map)
        (bt/make-return-map (assoc node :status :failure) entities tile-map)))))

;Commands
(defn- los? [ent1 ent2 curr-tile-map] 
  (let [grid-x1 (tile-map/world-coord->grid (:x (:transform ent1)))
        grid-y1 (tile-map/world-coord->grid (:y (:transform ent1)))
        grid-x2 (tile-map/world-coord->grid (:x (:transform ent2)))
        grid-y2 (tile-map/world-coord->grid (:y (:transform ent2)))
        tile-locs (math-utils/raytrace grid-x1 grid-y1
                                       grid-x2 grid-y2)
        are-tiles-passable? (map 
                              (fn [t-loc] (:passable (tile-map/get-tile (first t-loc) (second t-loc) curr-tile-map)))
                              tile-locs)
        have-los? (reduce #(and %1 %2) are-tiles-passable?)]
    have-los?))

;locate target
(defn- closest-visible-entity [main-ent-id entities curr-tile-map]
  "Returns the closest visible entity. If none found, nil."
  (let [main-ent (main-ent-id entities)
        qualifying-ents (into {} (filter (fn [e] (let [e-value (second e)]
                                                   (and (:hit-points e-value) (not= (:team e-value) (:team main-ent)))))
                                         entities)) 
        ents-los-checked (map 
                           (fn [e] 
                             {:entity e 
                              :have-los? (los? main-ent e curr-tile-map)})
                           (vals (dissoc qualifying-ents main-ent-id)))
        ents-los-true (filter #(:have-los? %) ents-los-checked)
        closest-ent (loop [e-los (rest ents-los-true)
                           closest-ent (:entity (first ents-los-true))]
                      (if (empty? e-los)
                        closest-ent
                        (recur (rest e-los)
                               (if (< (math-utils/distance (:entity (first e-los))
                                                           main-ent) 
                                      (math-utils/distance closest-ent
                                                           main-ent))
                                 (:entity (first e-los))
                                 closest-ent))))]
    closest-ent))

(defrecord LocateACombatTarget [status]
  bt/NodeBehavior
  (bt/reset [node]
       (assoc node :status :fresh))
  (bt/run [node main-ent-id entities curr-tile-map]
    (let [main-ent (main-ent-id entities)
          closest-ent (closest-visible-entity main-ent-id entities curr-tile-map)]
      (if (:id closest-ent)
        (bt/make-return-map (assoc node :status :success)
                            (assoc-in entities [main-ent-id :combat-target-id] (keyword (str (:id closest-ent))))
                            curr-tile-map)
        (bt/make-return-map (assoc node :status :failure)
                            entities
                            curr-tile-map)))))

(defrecord FindMeleeTarget [status]
  bt/NodeBehavior
  (bt/reset [node]
    (assoc node :status :fresh))
  (bt/run [node main-ent-id entities curr-tile-map]
    (let [main-ent (main-ent-id entities)
          closest-ent (closest-visible-entity main-ent-id entities curr-tile-map)]
      (if (:id closest-ent)
        (bt/make-return-map (assoc node :status :success)
                            (-> entities
                              (assoc main-ent-id (comps/move-to main-ent (:x (:transform closest-ent)) (:y (:transform closest-ent))))
                              (assoc-in [main-ent-id :combat-target-id] (keyword (str (:id closest-ent)))))
                            curr-tile-map)
        (bt/make-return-map (assoc node :status :failure)
                            entities
                            curr-tile-map)))))

;engage target
(defn- can-fire? [{p-weapon :projectile-weapon}]
  (if (<= (:curr-cooldown p-weapon) 0)
    true
    false))

(defn- fired-weapon [ent]
  (-> ent
    (assoc-in [:animation :current-animation] :fire-pistol)
    (assoc-in [:projectile-weapon :curr-cooldown] (get-in ent [:projectile-weapon :cooldown]))
    (dissoc :combat-target-id)))

(defn- pending-tracer-added [entities main-ent]
  (let [main-ent-transform (:transform main-ent)
        tracer-id (keyword (str (ecs/get-new-id)))
        tracer-x (+ (:x main-ent-transform) (:origin-x main-ent-transform))
        tracer-y (+ (:y main-ent-transform) (:origin-y main-ent-transform))
        combat-target-transform (:transform ((:combat-target-id main-ent) entities))
        combat-target-x (+ (:x combat-target-transform) (:origin-x combat-target-transform))
        combat-target-y (+ (:y combat-target-transform) (:origin-y combat-target-transform))
        scale-y (/ (math-utils/euclidean-distance tracer-x tracer-y
                                                  combat-target-x combat-target-y)
                 7)]
    (assoc entities 
           tracer-id (projectile/pending-entity 
                       #(projectile/tracer % 
                                           tracer-id 
                                           tracer-x
                                           tracer-y
                                           1 ;scale-x
                                           scale-y 
                                           (:rotation main-ent-transform))))))

(defn miss-shot? [target]
  (if-let [miss-chance (:chance-to-miss target)]
    (< (rand-int 100) miss-chance)
    false))

(defn- combat-target-damaged [entities main-ent]
  (let [target-id (:combat-target-id main-ent)]
    (if (miss-shot? (target-id entities))
      entities
      (update-in entities [(:combat-target-id main-ent) :hit-points] (fn [hp] 
                                                                       (- hp (:damage (:projectile-weapon main-ent))))))))
  
(defn- fire-weapon [main-ent-id entities]
  "Returns the full map of entities with the main entity and the target entity updated as a result of firing the main ent's weapon.
   Does not tick the cooldown for the weapon. Expecting a system to handle it because it will always tick every update."
  (if (can-fire? (main-ent-id entities))
    (let [main-ent (main-ent-id entities)
          updated-main-ent (fired-weapon main-ent)
          updated-entities (-> entities
                             (assoc main-ent-id updated-main-ent)
                             (combat-target-damaged main-ent)
                             (pending-tracer-added main-ent))]
      updated-entities)
    entities))

(defn- engage-combat [node main-ent-id entities curr-tile-map attack-fn]
  "attack-fn should not need any additional data/parameters. it must return an updated entity map"
  (if (nil? ((:combat-target-id (main-ent-id entities)) entities))
    (bt/make-return-map (assoc node :status :failure)
                        (update entities main-ent-id (fn [main-ent] (dissoc (main-ent-id entities) :combat-target)))
                        curr-tile-map)
    (let [main-ent (main-ent-id entities)
             target-ent ((:combat-target-id main-ent) entities)
             angle-to-face-target (-> 
                                    (math-utils/angle-of [(:x (:transform main-ent)) (:y (:transform main-ent))] [(:x (:transform target-ent)) (:y (:transform target-ent))])
                                    (math-utils/round-to-decimal 1))]
      (cond
        (not= (:rotation (:transform main-ent)) angle-to-face-target)
        (bt/make-return-map node
                            (assoc-in entities [main-ent-id :target-rotation] angle-to-face-target)
                            curr-tile-map)
        (los? main-ent target-ent curr-tile-map)
        (bt/make-return-map (assoc node :status :success)
                            (attack-fn)
                            curr-tile-map)
        :else 
        (bt/make-return-map (assoc node :status :failure)
                            (update entities main-ent-id (fn [main-ent] (dissoc main-ent :combat-target)))
                            curr-tile-map)))))

(defrecord EngageCombatTarget [status]
  bt/NodeBehavior
  (bt/reset [node] 
    (assoc node :status :fresh))
  (bt/run [node main-ent-id entities curr-tile-map]
    (engage-combat node main-ent-id entities curr-tile-map #(fire-weapon main-ent-id entities))))  

(defn- melee-attack [main-ent-id entities]
     (let [main-ent (main-ent-id entities)]
       (if (<= (:curr-cooldown (:melee-weapon main-ent)) 0)
         (let [launched-attack (-> main-ent
                                 (assoc-in [:animation :current-animation] :monster-attack)
                                 (assoc-in [:animation :current-frame] -1)
                                 (assoc-in [:melee-weapon :curr-cooldown] (get-in main-ent [:melee-weapon :cooldown]))
                                 (dissoc :combat-target-id))
               entities-w-launched (assoc entities main-ent-id launched-attack)
               entities-w-damaged (update-in entities-w-launched [(:combat-target-id main-ent) :hit-points] (fn [hp] 
                                                                                                              (- hp (:damage (:melee-weapon main-ent)))))]
           entities-w-damaged)
         entities)))

(defrecord MeleeCombatTarget [status]
  bt/NodeBehavior
  (bt/reset [node] 
    (assoc node :status :fresh))
  (bt/run [node main-ent-id entities curr-tile-map]
    (engage-combat node main-ent-id entities curr-tile-map #(melee-attack main-ent-id entities))))

(defrecord FindPath [status]
  bt/NodeBehavior
  (bt/reset [node]
    (assoc node :status :fresh))
  (bt/run [node main-ent-id entities curr-tile-map]
    (let [main-ent (main-ent-id entities)
          start-tile (tile-map/get-tile (tile-map/world-coord->grid (:x (:transform main-ent)))
                                        (tile-map/world-coord->grid (:y (:transform main-ent)))
                                        curr-tile-map)
          target-tile (tile-map/get-tile (:x (:move-to main-ent)) (:y (:move-to main-ent)) curr-tile-map)
          path (astar/calc-path start-tile target-tile curr-tile-map)]
      (if (seq? path)
        (bt/make-return-map (assoc node :status :success)
                            (assoc entities main-ent-id (-> main-ent 
                                                          (assoc :path (comps/path path))
                                                          (dissoc :move-to)))
                            curr-tile-map)
        (bt/make-return-map (assoc node :status :failure) main-ent curr-tile-map)))))

(defrecord PickRandomTile [status]
  bt/NodeBehavior
  (bt/reset [node]
       (assoc node :status :fresh))
  (bt/run [node main-ent-id entities curr-tile-map]
    (let [x-max 8
          y-max 8
          x (rand-int x-max)
          y (rand-int y-max)]
      (if (:passable (tile-map/get-tile x y curr-tile-map))
        (bt/make-return-map (assoc node :status :success) 
                            (update entities main-ent-id (fn [e] (comps/move-to e x y))) 
                            curr-tile-map)
        (bt/make-return-map (assoc node :status :failure)
                            entities
                            curr-tile-map)))))
  

(defrecord FollowPath [status]
  bt/NodeCancel
  (bt/cancel [node main-ent-id entities curr-tile-map]
    (bt/make-return-map (assoc node :status :failure)
                        (update entities main-ent-id (fn [old-ent] (dissoc old-ent 
                                                                           :path
                                                                           :target-location
                                                                           :target-rotation)))
                        curr-tile-map))
  bt/NodeBehavior
  (bt/reset [node] 
    (assoc node :status :fresh))
  (bt/run [node main-ent-id entities curr-tile-map]
    (let [main-ent (main-ent-id entities)
          curr-path-idx (get-in main-ent [:path :curr-path-idx])
          curr-path (get-in main-ent [:path :a-path])
          target-node (nth curr-path curr-path-idx nil)]
      (if (nil? target-node)
        (bt/make-return-map (assoc node :status :success) (assoc entities main-ent-id (dissoc main-ent :path)) curr-tile-map)
        (if (and 
              (== (:grid-x target-node) (tile-map/world-coord->grid (:x (:transform main-ent))))
              (== (:grid-y target-node) (tile-map/world-coord->grid (:y (:transform main-ent)))))
          ;this means we spend an AI tick just incrementing the curr path idx. ehh... didn't intend for this.
          (bt/make-return-map node (update-in entities [main-ent-id :path :curr-path-idx] #(inc %)) curr-tile-map)
          (let [ent-target-angle (math-utils/round-to-decimal 
                                   (math-utils/angle-of [(:x (:transform main-ent)) (:y (:transform main-ent))] 
                                                        [(tile-map/grid->world-coord (:grid-x target-node)) (tile-map/grid->world-coord (:grid-y target-node))]) 1)]
            (if (== ent-target-angle (:rotation (:transform main-ent)))
              ;walk towards it
              (if (:target-location main-ent)
                (bt/make-return-map node entities curr-tile-map)
                (bt/make-return-map node
                                    (assoc-in entities [main-ent-id :target-location] {:x (:grid-x target-node), :y (:grid-y target-node)})
                                    curr-tile-map))
              ;rotate towards it
              (if (:target-rotation main-ent)
                (bt/make-return-map node entities curr-tile-map)
                (bt/make-return-map node
                                    (assoc-in entities [main-ent-id :target-rotation] ent-target-angle)
                                    curr-tile-map)))))))))

;(defn- ents-in-fov [main-ent ents]
;  (let [main-e-collider (:fov-collider main-ent)
;        main-e-transform (:transform main-ent)
;        is-same-ent? (fn [e1 e2] (= (:id e1) (:id e2)))
;        local->world-coord (fn [collider transform]
;                             {:x (+ (:x collider) (:x transform))
;                              :y (+ (:y collider) (:y transform))
;                              :width (:width collider)
;                              :height (:height collider)})
;        ents-in-fov-nils (map (fn [e]
;                                (if (is-same-ent? main-ent e)
;                                  nil
;                                  (let [main-rect (local->world-coord main-e-collider main-e-transform)
;                                        e-rect (local->world-coord (:self-collider e) (:transform e))]
;                                    (if (math-utils/rectangle-overlap? main-rect e-rect)
;                                      e
;                                      nil)))) 
;                              ents)]
;    (vec (remove nil? ents-in-fov-nils))))