(ns basic-combat-ai.entities
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.components :as comps]
            [basic-combat-ai.enemy-ai-tree :as enemy-ai-tree]))

(defn pistoleer [{tex-cache :tex-cache} x y]
  "x and y need to be in terms of world coordinates. stop fucking putting in grid coords, chris.
although you should definately make it grid coords, because putting in world coordinates makes no fucking sense."
  (-> {} 
    (comps/hit-points 100)
    (comps/projectile-weapon :cooldown 0.8 
                             :curr-cooldown 0.0 
                             :damage 10 
                             :max-ammo 9 
                             :current-ammo 9 
                             :reload-speed 5)
    (comps/renderable (:pistol-idle tex-cache))
    (assoc 
      :transform (comps/transform x y 0 16 16)
      :movespeed (comps/movespeed 2 8)
      :animation (comps/animation
                   (comps/frames :pistol-idle (comps/frame (:pistol-idle tex-cache) 0.1) false
                                 :fire-pistol [(comps/frame (:fire-pistol01 tex-cache) 0.05)
                                               (comps/frame (:fire-pistol02 tex-cache) 0.05)
                                               (comps/frame (:pistol-idle tex-cache) 0.1)] false))
      :behavior-tree (comps/behavior-tree (enemy-ai-tree/basic-ai))
      
      ;this is a box, so that means yes, they have eyes on the back of their heads.
        ;doing this for simpilicity, and i'm not sure that having a cone fov is worth the extra work anyways.
        ;it's not like these guys can be controlled individually, using super advanced tactics or something.
        :fov-collider (comps/fov-collider -240 -240 480 480) ;7 tiles worth of view, each way
        :self-collider (comps/self-collider 0 0 32 32)
      )))

; (require '[basic-combat-ai.main-screen :as ms])
;(do 
;  (ms/update-game! #(ecs/add-entity % (pistoleer % 0 0)))
;  (ms/update-game! #(ecs/add-entity % (pistoleer % 32 0)))
;  (ms/update-game! #(ecs/add-entity % (pistoleer % 64 0)))
;  (ms/update-game! #(ecs/add-entity % (pistoleer % 64 64)))
;  (ms/update-game! #(ecs/add-entity % (pistoleer % 64 32))))
      
(defn init [game]
  (-> game
    ;(ecs/add-entity (pistoleer game 32 32))
    (ecs/add-entity (pistoleer game 128 128))
    ))