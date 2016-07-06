(ns basic-combat-ai.behavior-tree)

(defprotocol NodeBehavior
  "Node for a behavior tree. duh. The main-ent will also be in the game object, and will need to be updated in both spots. 
   It's just there so we don't have to keep retrieving it from the list of entities since most nodes will act on the main-ent."
  ;(start [node main-ent game] "Node will perform any sort of initializing behavior before it runs.")
  (reset [node] "reset to fresh state. do any clean up necessary.")
  (run [node main-ent-id ents tile-map] "Main node logic. Expected to set the node's status to success or failure."))

(defn make-return-map [node entities tile-map]
  {:node node
   :entities entities
   :tile-map tile-map})

(defn tick [node main-ent-id ents tile-map]
  (cond
    (= :fresh (:status node)) (run (assoc node :status :running)
										               main-ent-id
										               ents
										               tile-map)
    (= :running (:status node)) (run node
                                     main-ent-id
                                     ents
                                     tile-map)
    :else {:node node
           :entities ents
           :tile-map tile-map}))

(defrecord Selector [status curr-child-idx children]
  NodeBehavior
  (reset [node]
    (assoc node 
           :status :fresh
           :return-ents []
           :curr-child-idx 0))
  (run [node main-ent-id entities tile-map]
	  (let [current-child-idx (:curr-child-idx node)
         ;update current child
	        updated-child-data (tick (nth (:children node) current-child-idx) 
	                                 main-ent-id 
	                                 entities 
	                                 tile-map)
	        updated-child-status (:status (:node updated-child-data))
	        updated-children (assoc (:children node) current-child-idx (:node updated-child-data))
	        updated-child-ents (:entities updated-child-data)
	        updated-child-map (:tile-map updated-child-data)
	        child-successful-and-not-last? (and (= :success updated-child-status) (not= current-child-idx (dec (count (:children node)))))
	        selector-with-updated-children (assoc node :children updated-children)
          tick-updated-selector (fn [] (tick (assoc selector-with-updated-children :curr-child-idx (inc current-child-idx))
																				            main-ent-id
																				            updated-child-ents
																				            updated-child-map))
          selector-with-last-updated-child {:node (assoc selector-with-updated-children :status updated-child-status)
																		        :entities updated-child-ents
																		        :tile-map updated-child-map}]
	    (if child-successful-and-not-last?
	      (tick-updated-selector)
	      selector-with-last-updated-child))))
                              
(defrecord TestLeaf [status work-counter]
  NodeBehavior
  (reset [node]
    nil)
  (run [node main-ent-id entities tile-map]
    (if (= (:work-counter node) 1)
      {:node (assoc node :status :success) 
       :entities entities
       :tile-map tile-map}
      {:node (assoc node :work-counter (inc (:work-counter node))) 
       :entities entities
       :tile-map tile-map})))
            

;node helpers
;(defn reset-tree [node]
;  (if (empty? (:children node))
;    (reset node)
;    (reset (assoc node :children (mapv reset-tree (:children node)))))) 
;
;(defn update-node [node main-ent game]
;  (case (:status node)
;    :fresh (run (assoc node :status :running) main-ent game)
;    :running (run node main-ent game)
;    :success node
;    :failure node))

;selector helpers
;(defn- selector-success? [children]
;  (reduce #(and %1 %2) (map #(= :success (:status %)) children)))

;(defn- ent-updated? [ent-id modified-ents]
;  "If the old entity id is found in the list of modified entities, then it's assumed that it was updated."
;  (loop [new-ents modified-ents]
;	  (if (empty? new-ents)
;	    false
;	    (if (= (:id (first new-ents)) ent-id)
;	      true
;	      (recur (rest new-ents))))))
;
;(defn update-ents [ents modified-ents]
;  "Returns all entities in modified-ents + ents. Any entities that are in modified-ents will replace ones found in ents."
;  (loop [e ents
;         results modified-ents]
;    (if (empty? e)
;      results
;      (recur 
;        (rest e) 
;        (if (ent-updated? (:id (first e)) modified-ents) 
;          results ;then it's already in the results
;          (conj results (first e))))))) ;else add it 

;(defn- update-game-ents [game modified-ents]
;  (assoc game [:ecs :entities] (update-ents (get-in game [:ecs :entities]) modified-ents)))

;(defrecord Selector [status return-ents children curr-child-idx]
;  ;return-ents are only the ents that were modified. first ent is assumed to be the main-ent.
;  NodeBehavior
;  (reset [node]
;    (assoc node 
;           :status :fresh
;           :return-ents []
;           :curr-child-idx 0))
;  (run [node main-ent game]
;    (if (selector-success? children)
;      (assoc node :status :success)
;      (let [curr-child (nth children curr-child-idx)
;            updated-child (if (pos? (count (:return-ents node)))
;									          (update-node curr-child (first (:return-ents node)) (update-game-ents game (:return-ents node)))
;									          (update-node curr-child main-ent game))
;            updated-children (assoc children curr-child-idx updated-child)]
;        (case (:status updated-child)
;          :failure (assoc node 
;                          :status :failure 
;                          :children updated-children
;                          :return-ents (update-ents (:return-ents node) (:return-ents updated-child)))
;          :success (assoc node 
;                          :curr-child-idx (inc curr-child-idx)
;                          :children updated-children
;                          :return-ents (update-ents (:return-ents node) (:return-ents updated-child)))
;          :running (assoc node 
;                          :children updated-children
;                          :return-ents (update-ents (:return-ents node) (:return-ents updated-child))))))))


          
          
      
      
    
          
          
          
          
          
                          