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

(defn- box-collider
  "x and y are relative to the transform's x and y."
  [x y width height]
  {:x x
   :y y
   :width width
   :height height})

(defn fov-collider
  "x and y are relative to the transform's x and y."
  [x y width height]
  (box-collider x y width height))

(defn self-collider
  "x and y are relative to the transform's x and y."
  [x y width height]
  (box-collider x y width height))

(defn behavior-tree [root-node]
  {:tree root-node})

(defn move-to [x-tile y-tile]
  {:x x-tile
   :y y-tile})

(defn path [path]
  {:a-path path
   :curr-path-idx 0})

(defn movespeed [movespeed rotation-speed]
  {:movespeed movespeed
   :rotation-speed rotation-speed})

;target-rotation
;target-location
  
(defn hit-points [ent hp]
  (assoc ent :hit-points hp)) 

(defn projectile-weapon [ent & {:keys [cooldown curr-cooldown damage max-ammo current-ammo reload-speed]}]
     (assoc ent
            :projectile-weapon {:cooldown cooldown
                                :curr-cooldown curr-cooldown
                                :damage damage
                                :max-ammo max-ammo
                                :current-ammo current-ammo
                                :reload-speed reload-speed}))

(defn renderable 
  ([ent texture]
    (renderable ent texture 1 1))
  ([ent texture scale-x scale-y]
    (assoc ent 
           :renderable {:texture texture
                        :scale-x scale-x
                        :scale-y scale-y})))

(defn timed-life [ent life-in-seconds]
  (assoc ent
         :timed-life life-in-seconds))
  
 
  
  
  
  
  
  
  