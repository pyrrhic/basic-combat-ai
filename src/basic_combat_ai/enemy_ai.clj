(ns basic-combat-ai.enemy-ai
  (:require [basic-combat-ai.components :as comps]
            [basic-combat-ai.behavior-tree :as bt]
            [basic-combat-ai.tile-map :as tile-map]
            [basic-combat-ai.astar :as astar]
            [basic-combat-ai.math-utils :as math-utils]))

(defrecord HasMoveTo [status return-ents]
  bt/NodeBehavior
  (bt/reset [node]
    (assoc node
           :status :fresh
           :return-ents []))
  (bt/run [node main-ent game]
    (if (:move-to main-ent) (assoc node :status :success) (assoc node :status :failure))))

(defrecord FindPath [status return-ents]
  bt/NodeBehavior
  (bt/reset [node]
    (assoc node
           :status :fresh
           :return-ents []))
  (bt/run [node main-ent game]
    (let [start-tile (tile-map/get-tile (tile-map/world-coord->grid  (:x (:transform main-ent))) 
                                        (tile-map/world-coord->grid  (:y (:transform main-ent)))
                                        (:tile-map game))
          target-tile (tile-map/get-tile (:x (:move-to main-ent)) (:y (:move-to main-ent)) (:tile-map game))
          path (astar/calc-path start-tile target-tile (:tile-map game))]
      (if (seq? path)
        (assoc node 
               :return-ents (conj (:return-ents node) (-> main-ent 
                                                        (assoc-in [:path] (comps/path path))
                                                        (dissoc main-ent :move-to)))
               :status :success)
        (assoc node :status :failure)))))
  
(defrecord FollowPath [status return-ents]
  bt/NodeBehavior
  (bt/reset [node]
    (assoc node
           :status :fresh
           :return-ents []))
  (bt/run [node main-ent game]
    (let [curr-path-idx (get-in main-ent [:path :curr-path-idx])
          curr-path (get-in main-ent [:path :a-path])
          target-node (nth curr-path curr-path-idx nil)
          update-entity #(-> % 
	                         (assoc-in [:transform :x] (tile-map/grid->world-coord (:grid-x target-node)))
	                         (assoc-in [:transform :y] (tile-map/grid->world-coord (:grid-y target-node)))
	                         (assoc-in [:path :curr-path-idx] (inc curr-path-idx)))]
      (if (nil? target-node)
        (assoc node :status :success :return-ents [(dissoc main-ent :path)])
        (assoc node :return-ents [(update-entity main-ent)])))))

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