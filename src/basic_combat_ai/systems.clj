(ns basic-combat-ai.systems
  (:import [com.badlogic.gdx.graphics Texture OrthographicCamera] 
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.components :as comps]
            [basic-combat-ai.behavior-tree :as bt]))

(defn render [{{ents :entities} :ecs batch :batch cam :camera}]
  (let [qualifying-ents (filterv #(and (:renderable %) (:transform %)) ents)]
    (.setProjectionMatrix batch (.combined cam))
    (.begin batch)
    ;this loop isn't really any faster than using map. the slow part is the actual drawing done by libgdx/opengl
    (loop [q-ents qualifying-ents]
      (if (empty? q-ents)
        ents
        (let [e (first q-ents)
              texture-region (:renderable e)
						x (float (get-in e [:transform :x]))
						y (float (get-in e [:transform :y]))
						origin-x (float (get-in e [:transform :origin-x]))
						origin-y (float (get-in e [:transform :origin-y]))
						rotation (* -1 (float (get-in e [:transform :rotation]))) ;libgdx draws rotation counter clock wise, and um, i want to keep my code clock wise because it  makes more sense to me.
						  width (float (.getRegionWidth (:renderable e)))
						height (float (.getRegionHeight (:renderable e)))]
          (.draw batch texture-region x y origin-x origin-y width height (float 1) (float 1) rotation)
          (recur (rest q-ents)))))
    (.end batch))
  ents)

(defn- set-current-frame-num [entity cf-num]
  (assoc-in entity [:animation :current-frame] cf-num))

(defn- set-renderable [entity frame-num frames]
  (assoc entity :renderable (:texture (nth frames frame-num))))

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
                 tick-data (bt/tick (:tree (:behavior-tree (ent-id ents-m))) ent-id ents-m t-map)
                 updated-ents-m (:entities tick-data)
                 updated-ent-with-ticked-node (assoc-in (ent-id updated-ents-m) [:behavior-tree :tree] (:node tick-data))]
             ;somewhere i need to do a dirty update to the tile map, directly. yay global variables.
             (recur (rest q-ents) (assoc updated-ents-m ent-id updated-ent-with-ticked-node) (:tile-ap tick-data)))))))

;(defn- get-ent-pos [id ents*]
;     (loop [ents ents*
;            idx 0]
;       (if (= id (:id (first ents)))
;         idx
;         (recur (rest ents) (inc idx)))))

;(defn- replace-ent [idx replacement-ent ents]
;     (assoc ents idx replacement-ent))

;(defn tick-behavior-tree [{{ents :entities} :ecs :as game}]
;  "ticks the behavior tree once."
;  (let [[qualifying-ents rest-ents] (filter-ents ents #(:behavior-tree %))]
;    (loop [q-ents qualifying-ents
;           all-ents ents]
;      (if (empty? q-ents)
;        all-ents
;        (let [main-ent (first q-ents)
;              tree-root (get-in main-ent [:behavior-tree :tree])
;              ;check if the tree needs to be reset so it can be run again.
;              ready-root (if (or (= :success (:status tree-root))
;                                 (= :failure (:status tree-root)))
;                           (bt/reset-tree tree-root)
;                           tree-root)
;              ;the main-ent's BT will not be accessed. only the node that is passed in explicity.
;              updated-root (bt/update-node ready-root main-ent (assoc-in game [:ecs :entities] all-ents))
;              ;nothing in the nodes will put the BT back into the main-ent, because no node knows if it's the root, so we do it here.
;              ;also this is a function because we don't know if there are any return ents yet.
;              clear-return-ents (fn [root-node] (assoc root-node :return-ents []))
;              get-main-ent (fn [root-node] (first (:return-ents root-node)))
;              update-main-ent-bt (fn [root-node] (assoc-in (get-main-ent root-node) [:behavior-tree :tree] (clear-return-ents root-node)))
;              ]
;          ;for later--
;          ;if root comes back with success or fail, reset the whole tree then go through the tree.
;          ;if root comes back with running, go through the tree as if it were fresh. if land on an action node that is fresh, then need to cancel the old list of running nodes.
;          (if (empty? (:return-ents updated-root))
;            (recur (rest q-ents) (replace-ent
;                                   (get-ent-pos (:id main-ent) all-ents)
;                                   (assoc-in main-ent [:behavior-tree :tree] updated-root)
;                                   all-ents));(bt/update-ents all-ents [(assoc-in main-ent [:behavior-tree :tree] updated-root)]))
;            (recur (rest q-ents) (replace-ent
;                                   (get-ent-pos (:id main-ent) all-ents)
;                                   (update-main-ent-bt updated-root)
;                                   all-ents))
;            
;            ))))));(bt/update-ents all-ents [(update-main-ent-bt updated-root)]))))))))

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
															     target-rot-diff-rot-speed (- (Math/abs target-rot) rot-speed)
															     updated-rotation (if (or (pos? target-rot-diff-rot-speed) 
															                              (zero? target-rot-diff-rot-speed))
	                                                     ((if (neg? target-rot) - +) rot rot-speed)
	                                                     target-rot)
                                   rotated-ent (assoc-in q-ent [:transform :rotation] updated-rotation)]
	                              (if (== updated-rotation target-rot)
                                  (dissoc rotated-ent :target-rotation)
	                                rotated-ent)))
	                          qualifying-ents)]
	  (into modified-ents rest-ents)))

(defn init [game]
  (-> game
    (ecs/add-system render)
    (ecs/add-system animate)
    (ecs/add-system rotate)
    (ecs/add-system tick-behavior-tree)
    ))