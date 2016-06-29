(ns basic-combat-ai.desktop-launcher
  (:import [com.badlogic.gdx Gdx Application] 
           [com.badlogic.gdx.backends.lwjgl LwjglApplication LwjglApplicationConfiguration])
  (:require [basic-combat-ai.main-screen :as ms]
            [basic-combat-ai.CreateGame :as create-game]
            [clojure.tools.namespace.repl :as repl-tools]))

(defn set-main-screen []
  (.setScreen create-game/game (ms/screen)))

(defn run-on-main-thread [f]
  (.postRunnable Gdx/app f))

(defn reset []
  (run-on-main-thread set-main-screen))

;(defn refresh []
;  (do 
;	 (repl-tools/disable-reload! (find-ns 'basic-combat-ai.CreateGame))
;   (repl-tools/disable-reload! (find-ns 'basic-combat-ai.MyGame))
;	 (repl-tools/refresh)))

(def n '[basic-combat-ai.ecs
				basic-combat-ai.behavior-tree
				basic-combat-ai.components
				basic-combat-ai.tile-map
				basic-combat-ai.math-utils
				basic-combat-ai.astar
				basic-combat-ai.enemy-ai
				basic-combat-ai.entities
				basic-combat-ai.systems
				basic-combat-ai.main-screen
				basic-combat-ai.desktop-launcher])

(defn refresh [namespaces]
 (loop [nn namespaces]
   (if (empty? nn)
   nil
   (do
     (println "reloading " (first nn))
     (require (first nn) :reload)
     (recur (rest nn))))))

(defn app []
  (let [config (LwjglApplicationConfiguration.)]
    (set! (.height config) 600)
    (set! (.width config) 800)
    (set! (.title config) "hey you")
    (LwjglApplication. create-game/game config)))

;(app)
