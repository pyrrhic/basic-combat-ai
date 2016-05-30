(ns basic-combat-ai.MyGame
  (:import (com.badlogic.gdx Game Screen Gdx))
  (:require [basic-combat-ai.main-screen :as main-screen]))

(gen-class
 :name basic-combat-ai.MyGame
 :extends com.badlogic.gdx.Game)
 
(defn -create [^Game this]
  (.setScreen this (main-screen/screen)))