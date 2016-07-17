(ns basic-combat-ai.projectile
  (:require [basic-combat-ai.components :as comps]
            [basic-combat-ai.ecs :as ecs]))

(defn pending-entity [create-entity-fn]
  "create-entity-fn should be a function from the entity namespace.
   It should probably be a partial, with everything except for the game map (first param) filled in.
   The expectation is that a system will use this function to replace this entity with whatever entity 
   pops out of the function."
  {:pending-entity create-entity-fn})
  
(defn tracer [{tex-cache :tex-cache} id x y scale-x scale-y rotation]
  (-> {}
    (assoc :id id)
    (comps/renderable (:tracer tex-cache) scale-x scale-y)
    (comps/timed-life 0.01)
    (assoc :transform (comps/transform x y rotation 0 0))))