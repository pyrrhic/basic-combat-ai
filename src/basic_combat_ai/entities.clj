(ns basic-combat-ai.entities
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.components :as comps]))

(defn pistoleer [{tex-cache :tex-cache}]
  (assoc {}
         :transform (comps/transform 50 50 0 16 16)
         :renderable (:pistol-idle tex-cache)
         :animation (comps/animation
                    (comps/frames :pistol-idle (comps/frame (:pistol-idle tex-cache) 0.1) false
                                  :fire-pistol [(comps/frame (:fire-pistol01 tex-cache) 0.05)
                                                (comps/frame (:fire-pistol02 tex-cache) 0.05)
                                                (comps/frame (:pistol-idle tex-cache) 0.1)] false))
         ))

(defn init [game]
  (ecs/add-entity game (pistoleer game)))