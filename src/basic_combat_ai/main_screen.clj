(ns basic-combat-ai.main-screen
  (:import [com.badlogic.gdx Screen Gdx InputProcessor Input Input$Keys Input$Buttons]
           [com.badlogic.gdx.graphics Color Texture GL20] 
           [com.badlogic.gdx.graphics.g2d SpriteBatch TextureAtlas TextureRegion])
  (:require [basic-combat-ai.ecs :as ecs]
            [basic-combat-ai.entities :as ent]
            [basic-combat-ai.systems :as sys]))

(def game {})

(defn update-game! [func]
  "Expects a function with 1 parameter which will be the game map. The function must return the updated game map."
  (alter-var-root (var game) #(func %)))

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
     :pistol-idle (.findRegion atlas "pistol idle")}))

(defn init-game []
  (let [tex-cache (init-tex-cache)]
    (-> (assoc {}
               :batch (SpriteBatch.)
               :tex-cache tex-cache
               :inputs {})
      (ecs/init)
      (ent/init)
      (sys/init))))

(defn game-loop [game]
  (clear-screen)
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