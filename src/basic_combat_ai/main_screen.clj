(ns basic-combat-ai.main-screen
  (:import [com.badlogic.gdx Screen Gdx InputProcessor Input Input$Keys Input$Buttons]
           [com.badlogic.gdx.graphics Color Texture GL20 OrthographicCamera] 
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureAtlas TextureRegion]
           [java.util Date])
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.entities :as ent]
            [basic-combat-ai.systems :as sys]
            [basic-combat-ai.tile-map :as tile-map]))

(def game {})

(defn update-game! [func]
  "Expects a function with 1 parameter which will be the game map. The function must return the updated game map."
  (alter-var-root (var game) #(func %))
  nil)

(defn clear-screen []
  (doto (Gdx/gl)
    (.glClearColor 1 1 1 1)
    (.glClear GL20/GL_COLOR_BUFFER_BIT)))

(defn input-processor []
  (reify InputProcessor
    (touchDown [this x y pointer button] false)
    (keyDown [this keycode] 
      (alter-var-root (var game) #(assoc-in % [:inputs (keyword (Input$Keys/toString keycode))] true))
      true)
    (keyUp [this keycode] 
      (alter-var-root (var game) #(assoc-in % [:inputs (keyword (Input$Keys/toString keycode))] false))
      true)
    (keyTyped [this character] false)
    (touchUp [this x y pointer button] 
      (alter-var-root (var game) #(assoc-in % [:inputs :mouse-x] x))
      (alter-var-root (var game) #(assoc-in % [:inputs :mouse-y] (- 600 y)))
      false)
    (touchDragged [this x y pointer] false)
    (mouseMoved [this x y] false)
    (scrolled [this amount] false)))

(defn init-tex-cache []
  (let [atlas (TextureAtlas. "s.pack")]
    {:fire-pistol00 (.findRegion atlas "fire pistol00")
     :fire-pistol01 (.findRegion atlas "fire pistol01")
     :fire-pistol02 (.findRegion atlas "fire pistol02")
     :pistol-idle (.findRegion atlas "pistol idle")
     :floor (.findRegion atlas "floor")
     :wall (.findRegion atlas "wall")}))

(defn init-game []
  (let [tex-cache (init-tex-cache)]
    (-> {:batch (SpriteBatch.)
         :camera (OrthographicCamera. 800 600)
         :tex-cache tex-cache
         :inputs {}
         :tile-map (tile-map/create-grid 25 19 tex-cache)}
      (ecs/init)
      (ent/init)
      (sys/init))))

(def last-fps 0)
(def fps 0)
(def second-counter 0.0)

(defn game-loop [game]
  (def second-counter (+ second-counter (:delta game)))
  (def fps (inc fps))
  (when (>= second-counter 1.0)
    (do 
      (def second-counter 0.0)
      (def last-fps fps)
      (def fps 0)
      (when (< last-fps 60)
        (println "frame rate is dropping below 60 : " last-fps " @ " (new java.util.Date)))))
  
  (clear-screen)
  (.update (:camera game))
  (tile-map/draw-grid (:tile-map game) (:batch game))
  (-> game
    (ecs/update-ecs)))

(defn screen []
  (reify Screen
    (show [this]
      (.setInputProcessor Gdx/input (input-processor))
      (def game (init-game)))
    
    (render [this delta]
      (if (empty? game) ;if this file is reloaded in the repl, setScreen does not get called and bad things happen. So this avoids doing anything.
        ""
	      (do 
         (update-game! #(assoc % :delta delta))
         (update-game! #(game-loop %)))))
    
    (dispose[this])
    (hide [this])
    (pause [this])
    (resize [this w h])
    (resume [this])))