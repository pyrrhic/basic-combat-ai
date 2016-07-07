(ns basic-combat-ai.enemy-ai
  (:require [basic-combat-ai.components :as comps]
            [basic-combat-ai.behavior-tree :as bt]
            [basic-combat-ai.tile-map :as tile-map]
            [basic-combat-ai.astar :as astar]
            [basic-combat-ai.math-utils :as math-utils]))

(defrecord HasMoveTo [status]
     bt/NodeBehavior
     (bt/reset [node]
       (assoc node :status :fresh))
     (bt/run [node main-ent-id entities tile-map]
       (let [main-ent (main-ent-id entities)]
         (if (:move-to main-ent)
           (bt/make-return-map (assoc node :status :success) entities tile-map)
           (bt/make-return-map (assoc node :status :failure) entities tile-map)))))

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

(defrecord FollowPath [status]
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
          (bt/make-return-map node (update-in entities [main-ent-id :path :curr-path-idx] #(inc %)) curr-tile-map)
          (let [ent-target-angle (math-utils/angle-of [(:x (:transform main-ent)) (:y (:transform main-ent))] 
                                                      [(tile-map/grid->world-coord (:grid-x target-node)) (tile-map/grid->world-coord (:grid-y target-node))])]
            (if (== ent-target-angle (:rotation (:transform main-ent)))
              ;walk towards it
              (bt/make-return-map node
                                  (update-in entities [main-ent-id :transform] (fn [t] (assoc t 
                                                                                              :x (tile-map/grid->world-coord (:grid-x target-node))
                                                                                              :y (tile-map/grid->world-coord (:grid-y target-node)))))
                                  curr-tile-map)
              ;create component that will get picked up by a system to make us turn towards it.
              ;if there is ever a conflict with something else setting the target-rotation within the same frame, there will be overwriting or something. hmm.. not sure how i want to handle this yet.
                (bt/make-return-map node
                                    (assoc-in entities [main-ent-id :target-rotation] ent-target-angle)
                                    curr-tile-map))))))))

(defn- ents-in-fov [main-ent ents]
  (let [main-e-collider (:fov-collider main-ent)
        main-e-transform (:transform main-ent)
        is-same-ent? (fn [e1 e2] (= (:id e1) (:id e2)))
        local->world-coord (fn [collider transform]
                             {:x (+ (:x collider) (:x transform))
                              :y (+ (:y collider) (:y transform))
                              :width (:width collider)
                              :height (:height collider)})
        ents-in-fov-nils (map (fn [e]
                                (if (is-same-ent? main-ent e)
                                  nil
                                  (let [main-rect (local->world-coord main-e-collider main-e-transform)
                                        e-rect (local->world-coord (:self-collider e) (:transform e))]
                                    (if (math-utils/rectangle-overlap? main-rect e-rect)
                                      e
                                      nil)))) 
                              ents)]
    (vec (remove nil? ents-in-fov-nils))))