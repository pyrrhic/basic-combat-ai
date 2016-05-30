(ns basic-combat-ai.entities
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.components :as comps]))

(defn pistoleer [{tex-cache :tex-cache}]
  (assoc {}
         :transform (comps/transform 50 50 0 16 16)
         :renderable (:pistol-idle tex-cache)
         :state :idle)) ; :idle :firing

(defn init [game]
  (ecs/add-entity game (pistoleer game)))