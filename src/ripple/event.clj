(ns ripple.event
  (:require [ripple.subsystem :as s]
            [ripple.components :as c]
            [brute.entity :as e]
            [ripple.assets :as a]))

;; Event System

;; TODO - separate event queues on each EventHub instance isn't ideal
;; A single event queue for all entities should allow us to handle multiple event 
;; sequences in a single frame

(defn get-entities-with-tag 
  [system tag]
  (filter #(= (:tag (e/get-component system % 'EventHub))
              tag)
          (e/get-all-entities-with-component system 'EventHub)))

(defn send-event
  "Stores the given event in the event queue for the given entity"
  [system entity event]
  ;; TODO - assert that event has a value for :event-id
  (let [event-hub (e/get-component system entity 'EventHub)]
    (e/update-component system entity 'EventHub
                        #(assoc % :event-queue (conj (:event-queue event-hub) event)))))

(defn send-event-to-tag 
  [system tag event]
  (let [entities (get-entities-with-tag system tag)]
    (reduce #(send-event % %2 event)
            system entities)))

(defn- dispatch-event
  [system entity event]
  (let [components (e/get-all-components-on-entity system entity)
        event-handlers (->> (map #(get-in % [:on-event (:event-id event)]) components)
                            (filter #(not (nil? %))))]
    (if (> (count event-handlers) 0)
      (reduce (fn [system handler] (handler system entity event))
              system event-handlers)
      system)))

(defn- dispatch-events
  [system entity events]
  (reduce (fn [system event] (dispatch-event system entity event))
          system events))

(defn- update-event-hub
  [system entity]
  (let [events (:event-queue (e/get-component system entity 'EventHub))]
    (-> system
        (dispatch-events entity events)
        (e/update-component entity 'EventHub #(assoc % :event-queue [])))))

(defn- update-event-hubs
  [system]
  (let [entities (e/get-all-entities-with-component system 'EventHub)]
    (reduce update-event-hub system entities)))

(c/defcomponent EventHub 
  :fields [:tag {:default "untagged"}
           :outputs {:default []}
           :event-queue {:default []}])

(s/defsubsystem events
  :on-pre-render update-event-hubs
  :component-defs ['EventHub])
