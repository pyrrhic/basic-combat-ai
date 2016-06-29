(ns basic-combat-ai.entities
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.components :as comps]
            [basic-combat-ai.behavior-tree :as bt]
            [basic-combat-ai.enemy-ai :as enemy-ai]))

(defn pistoleer [{tex-cache :tex-cache} x y]
  (assoc {}
         :transform (comps/transform x y 0 16 16)
         :movespeed (comps/movespeed 1 3)
         :renderable (:pistol-idle tex-cache)
         :animation (comps/animation
                    (comps/frames :pistol-idle (comps/frame (:pistol-idle tex-cache) 0.1) false
                                  :fire-pistol [(comps/frame (:fire-pistol01 tex-cache) 0.05)
                                                (comps/frame (:fire-pistol02 tex-cache) 0.05)
                                                (comps/frame (:pistol-idle tex-cache) 0.1)] false))
         :behavior-tree (comps/behavior-tree (bt/->Selector :fresh [] [(enemy-ai/->HasMoveTo :fresh [])
																						                          (enemy-ai/->FindPath :fresh [])
																						                          (enemy-ai/->FollowPath :fresh [])] 0))
         ;this is a box, so that means yes, they have eyes on the back of their heads.
         ;doing this for simpilicity, and i'm not sure that having a cone fov is worth the extra work anyways.
         ;it's not like these guys can be controlled individually, using super advanced tactics or something.
         :fov-collider (comps/fov-collider -240 -240 480 480) ;7 tiles worth of view, each way
         :self-collider (comps/self-collider 0 0 32 32)
         ))

;(defn init [game]
;  (loop [g game
;         counter 2]
;    (if (zero? counter)
;      g
;      (recur (ecs/add-entity g (pistoleer g 0 0))
;             (dec counter)))))
      
(defn init [game]
  (-> game
    (ecs/add-entity (pistoleer game 0 0))
    (ecs/add-entity (pistoleer game 64 96))
    ))