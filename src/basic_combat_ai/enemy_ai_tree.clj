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

(defn basic-defender-ai []
  (->
    (bt/->Sequence :fresh 0
                 [(enemy-ai/->LocateACombatTarget :fresh)
                  (enemy-ai/->EngageCombatTarget :fresh)])
    (bt/add-ids-to-tree)))

(defn basic-attacker-ai []
  (-> 
    (bt/->Selector :fresh 0
                   [(bt/->Sequence :fresh 0
                                   [(enemy-ai/->LocateACombatTarget :fresh)
                                    (enemy-ai/->EngageCombatTarget :fresh)])
                    (bt/->Sequence :fresh 0 
                                   [(enemy-ai/->HasMoveTo :fresh)
                                    (enemy-ai/->FindPath :fresh)
                                    (enemy-ai/->FollowPath :fresh)])])
    (bt/add-ids-to-tree)))

(defn zergling-ai []
  (-> 
    (bt/->Sequence :fresh 0 
                   [(enemy-ai/->FindMeleeTarget :fresh)
                    (enemy-ai/->FindPath :fresh)
                    (enemy-ai/->FollowPath :fresh)
                    (enemy-ai/->CombatTargetAlive :fresh)
                    (enemy-ai/->MeleeCombatTarget :fresh)])
    (bt/add-ids-to-tree)))



  
  
