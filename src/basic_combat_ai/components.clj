(ns basic-combat-ai.components)

(defn transform [x y rot orig-x orig-y]
  (assoc {}
         :x x
         :y y
         :rotation rot
         :origin-x orig-x
         :origin-y orig-y))

(defn frame [tex dur]
  {:texture tex 
   :duration dur})

(defn frames [& args]
           (let [mapped (map (fn [name-frame]
                               {(first name-frame) (let [second-item (second name-frame)]
                                                     (if (vector? second-item) 
                                                       second-item 
                                                       (vector second-item)))})
                             (partition 2 args))]
           (reduce conj mapped)))

(defn animation [frames loop?]
  (assoc {}
         :current-frame -1
         :current-duration 0.0
         :loop? loop?
         :frames frames))