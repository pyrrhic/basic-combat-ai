(ns basic-combat-ai.behavior-tree)

;node
;(defn create-node []
;  {:status fresh})
;
;leaves
;(defn find-path []


;composites
(defn selector []
  {:status :fresh
   :children []
   :curr-child-idx 0})

(defn selector-run [selector]
  (let [curr-child (nth (:children selector) (:curr-child-idx selector))]
    (case (:status curr-child)
      :fresh

(defn selector-start [selector]
  (-> selector
    (assoc :curr-child-idx 0)
    (assoc :status :running)
    (selector-run)))