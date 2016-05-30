(ns basic-combat-ai.ecs)

(defn init [game]
  "Returns game map with ecs map added."
    (assoc game :ecs {:entities [], :systems []}))

(defn add-system [{{existing-systems :systems} :ecs :as game} new-systems]
  "Returns game map with system functions added.
   Systems must return a vector of entity maps."
  (let [ass-game (partial assoc-in game [:ecs :systems])]
    (if (vector? new-systems)
      (ass-game (into existing-systems new-systems))
      (ass-game (conj existing-systems new-systems)))))

(defn update-ecs [{{entities :entities systems :systems} :ecs :as game}]
  "Returns game map with entities that have had all the system functions executed on them."
     (assoc-in game [:ecs :entities] (loop [syss systems
                                           ents entities]
                                      (if (empty? syss)
                                        ents
                                        (recur (rest syss) ((first syss) (assoc-in game [:ecs :entities] ents)))))))

(defn add-entity [{{ents :entities} :ecs :as game} new-entity]
  "Returns game map with entity added."
  (assoc-in game [:ecs :entities] (conj ents new-entity)))