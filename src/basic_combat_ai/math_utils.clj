(ns basic-combat-ai.math-utils
  (:import [com.badlogic.gdx.math Intersector]))

;this is a test.
(defn convex-polygon-intersect? []
  (let [p1 (float-array [1 1
                         1 2
                         2 1])
        p2 (float-array [0 0
                         10 10
                         20 20])]
    (Intersector/overlapConvexPolygons p1 0 (count p1) p2 0 (count p2) nil)))

(defn bind-0-359 [angle-degrees]
  (cond 
    (neg? angle-degrees) 
      (+ 360 angle-degrees)
      
    (> angle-degrees 359) 
      (- angle-degrees 360)
      
    :else 
      angle-degrees))

(defn angle-of [p2 p1]
  ;add check to see if the points are the same. if so, return nil because valid numbers are 0-359.
  ;and this scenario would generate 0, which isn't correct. 0 means north.
  (let [delta-y (- (first p1) (first p2))
        delta-x (- (second p1) (second p2))
        angle-radians (Math/atan2 delta-y delta-x)
        angle-degrees (Math/toDegrees angle-radians)
        bound-0-360 (bind-0-359 angle-degrees)]
     bound-0-360))

(defn angle-of-unbounded [p2 p1]
  (let [delta-y (- (first p1) (first p2))
        delta-x (- (second p1) (second p2))
        angle-radians (Math/atan2 delta-y delta-x)
        angle-degrees (Math/toDegrees angle-radians)]
     angle-degrees))

(defn rectangle-overlap? [r1 r2]
  "Stole this from the Intersector object in libgdx. Wanted to avoid having to instantiate Rectangle objects
   when they arn't really used anywhere else. Initially I thought it would be performance overhead as well, but not sure."
  (let [{x1 :x, y1 :y, width1 :width, height1 :height} r1
        {x2 :x, y2 :y, width2 :width, height2 :height} r2]
    (and 
      (< x1 (+ x2 width2))
      (> (+ x1 width1) x2)
      (< y1 (+ y2 height2))
      (> (+ y1 height1) y2))))

(defn raytrace [x0 y0 x1 y1]
      "http://playtechs.blogspot.com/2007/03/raytracing-on-grid.html 
      Took the algorithm at the bottom, the all integer version. It looks like the Bresenham algorithm.
      I modified it slightly to include the scenario when error is 0, which means the line is exactly inbetween tiles.
      Basically the tile for that loop gets skipped, instead of the defaulting to the horizontal tile like the original algorithm.
      My modification makes this ray sorta mimick entity movement.
      Returns something like this [[x y] [x y] [x y]]. It's in order, based on the input."
      (let [dx (Math/abs (- x1 x0))
            dy (Math/abs (- y1 y0))
            x-inc (if (> x1 x0) 1 -1)
            y-inc (if (> y1 y0) 1 -1)
            dx2 (* dx 2)
            dy2 (* dy 2)]
        (loop [x x0
               y y0
               n (+ 1 dx dy)
               err (- dx dy) 
               result []]
          (if (<= n 0)
            result
            (cond 
              (pos? err) (recur (+ x x-inc) y (dec n) (- err dy2) (conj result [x y]))
              (neg? err) (recur x (+ y y-inc) (dec n) (+ err dx2) (conj result [x y]))
              ;when err = 0, the thing i added.
              :else (recur (+ x x-inc) (+ y y-inc) (- n 2) (- (+ err dx2) dy2) (conj result [x y])) 
              )))))

;(let [positions (raytrace 0 0 6 2)
;      wall-texture (:wall (:tex-cache ms/game))
;      floor-texture (:floor (:tex-cache ms/game))
;      update-tile (fn [x y] (assoc-in (:tile-map ms/game) [x y :texture] wall-texture))]
;  (loop [posis positions]
;    (if (empty? posis)
;      nil
;      (do
;        (let [tile-pos (nth posis 0)
;              x (nth tile-pos 0)
;              y (nth tile-pos 1)]
;        (ms/update-game! #(assoc % :tile-map (update-tile x y)))
;        (recur (rest posis)))))))

;rotate p2 around center
;newX = centerX + ( cosX * (point2X-centerX) + sinX * (point2Y -centerY))
;newY = centerY + ( -sinX * (point2X-centerX) + cosX * (point2Y -centerY))