(ns basic-combat-ai.systems_test
  (:require [clojure.test :refer :all]
            [basic-combat-ai.entities :as ent]
            [basic-combat-ai.systems :refer :all]))

;animate
(deftest no-current-animation
     "no changes should occur if an entity does not have a current animation"
     (let [e (assoc (ent/pistoleer nil) :renderable "")]
       (is (= e 
              (first (animate {:ecs {:entities [e]}}))))))

(deftest start-the-animation
  "Given an animation that has not been started, well uh, START IT."
  (let [e (-> (ent/pistoleer nil)
            (assoc-in [:animation :current-animation] :pistol-idle)
            (assoc :renderable ""))
	      current-animation (get-in e [:animation :current-animation])
	      current-frames (get-in e [:animation :frames current-animation :frame-duration])
        mock-game {:ecs {:entities [e]}}
        animated-e (first (animate mock-game))]
    (is (and (zero? (get-in animated-e [:animation :current-frame])) 
             (== (get-in animated-e [:animation :current-duration]) (:duration (first current-frames)))))))

(deftest continue-running-animation
  "If an animation is running and still has duration left, subtract delta from the duration"
  (let [e (-> (ent/pistoleer nil)
            (assoc-in [:animation :current-animation] :pistol-idle)
            (assoc :renderable "")
            (assoc-in [:animation :current-frame] 0)
            (assoc-in [:animation :current-duration] 1))
        mock-game {:ecs {:entities [e]} :delta 0.1}
        animated-e (first (animate mock-game))]
    (is (= (get-in animated-e [:animation :current-duration]) 0.9))))

(deftest loop-when-last-frame-done
  "If an animation has finished running the last frame and it supposed to loop, start back at the first frame."
  (let [e (-> (ent/pistoleer nil)
            (assoc-in [:animation :current-animation] :fire-pistol)
            (assoc :renderable "")
            (assoc-in [:animation :frames :fire-pistol :loop?] true)
            (assoc-in [:animation :current-frame] 2)
            (assoc-in [:animation :current-duration] -1.0))
        mock-game {:ecs {:entities [e]} :delta 0.1}
        animated-e (first (animate mock-game))]
    (is (zero? (get-in animated-e [:animation :current-frame])))
    (is (pos? (get-in animated-e [:animation :current-duration])))
    ))

(deftest set-to-next-frame
  "When a frame has finished running but there are still more frames in the animation, set shit to the next frame."
  (let [e (-> (ent/pistoleer nil)
            (assoc-in [:animation :current-animation] :fire-pistol)
            (assoc :renderable "")
            (assoc-in [:animation :current-frame] 0)
            (assoc-in [:animation :current-duration] -1.0))
        mock-game {:ecs {:entities [e]} :delta 0.1}
        animated-e (first (animate mock-game))]
    (is (= 1 (get-in animated-e [:animation :current-frame])))
    (is (pos? (get-in animated-e [:animation :current-duration])))))

(deftest return-vector-of-all-entities
	 "All entities, whether they were modified or not, are returned."
	 (let [e (-> (ent/pistoleer nil)
	           (assoc-in [:animation :current-animation] :fire-pistol)
	           (assoc :renderable "")
	           (assoc-in [:animation :current-frame] 0)
	           (assoc-in [:animation :current-duration] -1.0))
	       not-qualified-e {:im-not-empty-map ""}
	       mock-game {:ecs {:entities [e not-qualified-e]} :delta 0.1}
	       entities (animate mock-game)]
	   (is (= 2 (count entities))
     (is (some #{not-qualified-e} entities)))))

