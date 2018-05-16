(ns node-graph-algorithm.core
    (:require
      [cljsjs.viz :as viz]
      [cljsjs.nacl-fast :as nacl]
      [reagent.core :as r]))

; turn these into UI options
(def introducer-nodes 3)
(def peer-pool-size 4)
(def node-count 25)

(defn hexenate [b]
  (.join (.map (js/Array.from (.slice b)) #(.slice (str "0" (.toString (bit-and % 0xff) 16)) -2)) ""))

(defn make-node [nodes]
  (let [keypair (nacl.sign.keyPair)
        pk (.substr (hexenate (.-publicKey keypair)) 0 8)]
    {:pk pk
     :keys keypair
     :connections (map :pk (take peer-pool-size (random-sample 0.5 nodes)))}))

(defn order-connections [nodes new-node]
  
  )

(defn introduce-new-node [nodes new-node]
  (let [connections (new-node :connections)]
    ; ...
    nodes))

(defn graph-to-viz [nodes]
  (str "digraph G {\n"
       (apply str
              (for [node nodes child (node :connections)]
                (str "\tn" (name (node :pk)) " -> n" (name child) "\n")))
       "}"))

(defn component-render-graph [nodes]
  [:div [:h2 "swarm connectivity"]
   (let [v (graph-to-viz nodes)]
     (print v)
     [:div
      [:div {:dangerouslySetInnerHTML {:__html (viz v #js {"format" "svg" "engine" "fdp"})}}]
      [:pre v]])])

(defn make-nodes [n]
  (loop [remaining n nodes []]
    (if (> remaining 0)
      (let [new-node (make-node nodes)
            nodes (introduce-new-node nodes new-node)]
        (recur
          (dec remaining)
          (conj nodes new-node)))
      nodes)))

;; -------------------------
;; Initialize app

(defn mount-root []
  (let [nodes (make-nodes node-count)]
    (r/render [component-render-graph nodes] (.getElementById js/document "app"))))

(defn init! []
  (mount-root))
