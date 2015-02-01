(ns ripple.leaks.player
  (:require [brute.entity :as e]
            [ripple.components :as c]
            [ripple.rendering :as r]
            [ripple.subsystem :as s])
  (:import [com.badlogic.gdx.math Vector2]
           [com.badlogic.gdx Input$Keys]
           [com.badlogic.gdx Gdx]))

(c/defcomponent Player
  :fields [:jet-force {:default 100}])

(defn- get-move-direction []
  "Return normalized movement direction for whatever movement keys are currently depressed"
  (let [keys-to-direction {Input$Keys/DPAD_UP (Vector2. 0 1)
                           Input$Keys/DPAD_DOWN (Vector2. 0 -1)
                           Input$Keys/DPAD_LEFT (Vector2. -1 0)
                           Input$Keys/DPAD_RIGHT (Vector2. 1 0)
                           Input$Keys/W (Vector2. 0 1)
                           Input$Keys/S (Vector2. 0 -1)
                           Input$Keys/A (Vector2. -1 0)
                           Input$Keys/D (Vector2. 1 0)}]
    (-> (reduce (fn [move-direction [keycode direction]]
                  (if (.isKeyPressed Gdx/input keycode)
                    (.add move-direction direction)
                    move-direction))
                (Vector2. 0 0) keys-to-direction)
        (.nor))))

(defn- apply-jet-force
  [system entity direction]
  (let [body (-> (e/get-component system entity 'PhysicsBody)
                 (:body))
        force (-> (e/get-component system entity 'Player)
                  (:jet-force))
        force (.scl direction (float force))]
    (.applyForceToCenter body force true)))

(defn- update-player-movement
  [system entity]
  (let [direction (get-move-direction)]
    (when (> (.len2 direction) 0)
      (apply-jet-force system entity direction))
    system))

(defn- update-camera-target
  [system entity]
  (let [[x y] (:position (e/get-component system entity 'Transform))
        camera (get-in system [:renderer :camera])]
    (r/update-camera-position camera x y))
  system)

(defn- update-player
  [system entity]
  (let [player (e/get-component system entity 'Player)]
    (-> system
        ;(update-player-aim entity)
        (update-player-movement entity)
        (update-camera-target entity))))

(defn- update-player-components
  [system]
  (let [player-entities (e/get-all-entities-with-component system 'Player)]
    (reduce update-player system player-entities)))

(s/defsubsystem player
  :on-show
  (fn [system]
    (c/register-component-def 'Player Player)
    system)
  :on-pre-render update-player-components)
