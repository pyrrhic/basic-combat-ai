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
              (.draw batch texture-region x y origin-x origin-y width height (float 1) (float 1) rotation)) ;the 1's are scale x and scale y
           qualifying-ents))
    (.end batch))
  ents)

;(defn pistoleer-state->anim-state [{{entities :entities} :ecs}]
;  (let [qualifying-ents (filterv #(and (:state %) (:

(defn init [game]
  (ecs/add-system game render))