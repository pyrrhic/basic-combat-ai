(ns basic-combat-ai.math-utils
  (:import [com.badlogic.gdx.math Intersector]))

(defn convex-polygon-intersect? []
  (let [p1 (float-array [1 1
                         1 2
                         2 1])
        p2 (float-array [0 0
                         10 10
                         20 20])]
    (Intersector/overlapConvexPolygons p1 0 (count p1) p2 0 (count p2) nil)))

(defn angle-of [p1 p2]
  (let [delta-y (- (second p1) (second p2))
        delta-x (- (first p1) (first p2))
        angle-radians (Math/atan2 delta-y delta-x)
        angle-degrees (Math/toDegrees angle-radians)
        bound-0-360 (if (neg? angle-degrees) (+ 360 angle-degrees) angle-degrees)]
     bound-0-360))

;rotate p2 around center
;newX = centerX + ( cosX * (point2X-centerX) + sinX * (point2Y -centerY))
;newY = centerY + ( -sinX * (point2X-centerX) + cosX * (point2Y -centerY))