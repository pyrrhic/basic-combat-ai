(ns basic-combat-ai.enemy-ai-tree
  (:require [basic-combat-ai.behavior-tree :as bt]
            [basic-combat-ai.enemy-ai :as enemy-ai]))

(defn basic-ai []
  (-> 
    (bt/->Selector :fresh 0 
                   [(bt/->Sequence :fresh 0
                                   [(enemy-ai/->LocateACombatTarget :fresh)
                                    (enemy-ai/->EngageCombatTarget :fresh)])
                    (bt/->Sequence :fresh 0 
                                   [(enemy-ai/->PickRandomTile :fresh)
                                    (enemy-ai/->FindPath :fresh)
                                    (enemy-ai/->FollowPath :fresh)])])
    (bt/add-ids-to-tree)))
  
