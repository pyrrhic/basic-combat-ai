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
	"name of frames, frames, loop?
	 ex.
	 :name1 [f f f] true :name2 [f f] false :name 3 f true"
 (let [mapped (map (fn [name-frame]
                     {(first name-frame) 
                      {:frame-duration (let [second-item (second name-frame)]
	                                       (if (vector? second-item) 
									                         second-item 
									                         (vector second-item)))
                       :loop? (nth name-frame 2)}})
                   (partition 3 args))]
 (reduce conj mapped)))

(defn animation [frames]
  {:current-frame -1
   :current-duration 0.0
   :current-animation nil
   :frames frames})

(defn collider
  "x and y are relative to the transform's x and y. 
   vertices is a vector, example: [x1 y1 x2 y2 ...]"
  ([x y width height vertices]
  {:x x
   :y y
   :width width
   :height height
   :vertices vertices})
  ([collider-name x y width height vertices]
    (assoc (collider x y width height vertices)
           :name collider-name)))

(defn movement []
  {:rotation-speed 5})
  
  
  
  
  
  
  
  
  
  
  