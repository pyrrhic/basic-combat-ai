(ns basic-combat-ai.behavior-tree)

;protocols
(defprotocol NodeBehavior
  "Node for a behavior tree. duh. The main-ent will also be in the game object, and will need to be updated in both spots. 
   It's just there so we don't have to keep retrieving it from the list of entities since most nodes will act on the main-ent."
  ;(start [node main-ent game] "Node will perform any sort of initializing behavior before it runs.")
  (reset [node] "reset to fresh state.")
  (run [node main-ent-id ents tile-map] "Main node logic. Expected to set the node's status to success or failure."))

(defprotocol NodeCancel
  "Reverses any shit it needs to, OK?!?!?!"
  (cancel [node main-ent-id ents tile-map]))
  
;id's of nodes
(def node-id-counter (atom 0N))

(defn get-new-id []
  (swap! node-id-counter inc)
  @node-id-counter)

(defn add-ids-to-tree [node]
  (let [children (:children node)
        node-with-id (assoc node :id (get-new-id))]
    (if-not children
      node-with-id
      (assoc node-with-id :children (mapv add-ids-to-tree children)))))

(defn get-id-of-running-composite [node]
  (loop [unexplored [node]
         composites []]      
      (if (empty? unexplored)
        (:id (peek composites))
        (let [unexp (first unexplored)
              rest-unexplored (rest unexplored)]
          (recur (if (:children unexp) 
                   (into rest-unexplored (:children unexp))
                   rest-unexplored)
                 (if (and (:children unexp) (= (:status unexp) :running))
                   (conj composites unexp)
                   composites))))))

;utils
(defn make-return-map [node entities tile-map]
  {:node node
   :entities entities
   :tile-map tile-map})

(defn cancel-running-leaf [node main-ent-id ent-m tile-map]
  "Calls cancel on the running leaf node.
   Returns: {:node nil ;this is always nil, cancel is used for updating the ents/map not the tree
             :entities {...}
             :tile-map {...}
   Assumptions: There is only one leaf in a tree that is :running."
  (loop [unexplored [node]]
    (if (empty? unexplored)
      ;There was nothing to cancel.
      (make-return-map nil ent-m tile-map)
      (let [unexp (first unexplored)]
        (cond 
          (and (not (:children unexp)) (= (:status unexp) :running) (satisfies? NodeCancel unexp))
            (cancel unexp main-ent-id ent-m tile-map)
          (and (:children unexp) (= (:status unexp) :running))
            (recur (into (rest unexplored) (:children unexp)))
          :else 
            (recur (rest unexplored)))))))

(defn reset-behavior-tree [node]
  (if-not (:children node)
    (reset node)
    (-> node
      (reset)
      (update :children #(mapv reset-behavior-tree %)))))

(defn tick [node main-ent-id ents tile-map]
  "returns {:node ;this is the updated root node 
            :entities ;any entities that were updated. behavior trees are not updated, only 1 tree will be updated
                      ;and that's the main ent, who's bt is in :node.
            :tile-map ;the potentially updated tile-map.}"
  (cond
    (= :fresh (:status node)) (run (assoc node :status :running) main-ent-id ents tile-map)
    (= :running (:status node)) (run node main-ent-id ents tile-map)
    :else {:node node
           :entities ents
           :tile-map tile-map}))

;composites
(defrecord Selector [status curr-child-idx children]
  ;it works like a logical OR
  NodeBehavior
  (reset [node]
    (assoc node 
           :status :fresh
           :curr-child-idx 0))
  (run [node main-ent-id entities tile-map]
    (let [current-child-idx (:curr-child-idx node)
	        updated-child-data (tick (nth (:children node) current-child-idx) 
	                                 main-ent-id 
	                                 entities 
	                                 tile-map)
	        updated-child-status (:status (:node updated-child-data))
	        updated-children (assoc (:children node) current-child-idx (:node updated-child-data))
	        updated-child-ents (:entities updated-child-data)
	        updated-child-map (:tile-map updated-child-data)
	        selector-with-updated-children (assoc node :children updated-children)
          tick-updated-selector (fn [] (tick (assoc selector-with-updated-children :curr-child-idx (inc current-child-idx))
																				            main-ent-id
																				            updated-child-ents
																				            updated-child-map))]
      (cond
        (= :running updated-child-status) 
          (make-return-map (assoc selector-with-updated-children :status :running)
                           updated-child-ents
                           updated-child-map)
        (= :success updated-child-status)
          (make-return-map (assoc selector-with-updated-children :status :success)
                           updated-child-ents
                           updated-child-map)
        (not= current-child-idx (dec (count (:children node))))
          (tick-updated-selector)
        :else ;not running, not success, is last child.
          (make-return-map (assoc selector-with-updated-children :status :failure)
                           updated-child-ents
                           updated-child-map)))))

(defrecord Sequence [status curr-child-idx children]
  ;works like logical AND
  NodeBehavior
  (reset [node]
    (assoc node 
           :status :fresh
           :curr-child-idx 0))
  (run [node main-ent-id entities tile-map]
	  (let [current-child-idx (:curr-child-idx node)
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

;(defrecord LoopUntil [status curr-child-idx children]
;  NodeBehavior
;  (reset [node]
;    (assoc node 
;           :status :fresh
;           :curr-child-idx 0))
;  (run [node main-ent-id entities tile-map]
;    (every #(= :success (:status %)) children)

(defrecord TestLeaf [status work-counter]
  NodeCancel 
  (cancel [node main-ent-id entities tile-map]
    (make-return-map nil ;node is irrelevant
                     (assoc-in entities [main-ent-id :canceled] true)
                     tile-map))
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


          
          
      
      
    
          
          
          
          
          
                          