(ns basic-combat-ai.systems
  (:import [com.badlogic.gdx.graphics Texture] 
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureRegion])
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.components :as comps]))

(defn render [{{ents :entities} :ecs batch :batch}]
  (let [qualifying-ents (filterv #(and (:renderable %) (:transform %)) ents)]
    (.begin batch)
    (doall 
      (map #(let [texture-region (:renderable %)
                  x (float (get-in % [:transform :x]))
                  y (float (get-in % [:transform :y]))
                  origin-x (float (get-in % [:transform :origin-x]))
                  origin-y (float (get-in % [:transform :origin-y]))
                  rotation (float (get-in % [:transform :rotation]))
                  width (float (.getRegionWidth (:renderable %)))
                  height (float (.getRegionHeight (:renderable %)))]
              (.draw batch texture-region x y origin-x origin-y width height (float 1) (float 1) rotation)) ;the 1's are params scale x and scale y
           qualifying-ents))
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

(defn init [game]
  (ecs/add-system game render))