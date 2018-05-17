(ns node-graph-algorithm.core
    (:require
      [cljsjs.viz :as viz]
      [cljsjs.nacl-fast :as nacl]
      [reagent.core :as r]))

; turn these into UI options
(def introducer-peer-count 5) ; more introducers means more diversity
(def peer-pool-size 8) ; more peers means more diversity
(def node-count 20)
(def with-connection-ordering true)

(defn hexenate [b]
  (.join (.map (js/Array.from (.slice b)) #(.slice (str "0" (.toString (bit-and % 0xff) 16)) -2)) ""))

(defn unhexenate [b]
  (js/Uint8Array. (map #(js/parseInt (apply str %) 16) (partition 2 b))))

(defn join-uint8arrays [a b]
  (let [n (js/Uint8Array. (+ (.-length a) (.-length b)))]
    (.set n a)
    (.set n b (.-length a))
    n))

(defn make-node [index]
  (let [keypair (nacl.sign.keyPair)
        pk (.substr (hexenate (.-publicKey keypair)) 0 8)]
    {:pk pk
     :keys keypair
     :connections #{}
     :index index}))

(defn add-connection [me new-node]
  (let [connections (me :connections)
        connections (conj connections (new-node :pk))
        connections (if with-connection-ordering
                      (sort-by #(hexenate (nacl.hash (join-uint8arrays (unhexenate (me :pk)) (unhexenate %)))) connections)
                      connections)
        connections (set (take peer-pool-size connections))]
    (assoc me :connections connections)))

(defn introduce-new-node [nodes new-node]
  (let [connections (new-node :connections)]
    (map
      #(if (contains? connections (% :pk))
         (add-connection % new-node)
         %)
      nodes)))

(defn select-initial-nodes [node introducer-nodes]
  ;(print "introducer-nodes:" introducer-nodes)
  (loop [updated-node node remaining-nodes introducer-nodes]
    (if (> (count remaining-nodes) 0)
      (recur (add-connection updated-node (first remaining-nodes))
             (rest remaining-nodes))
      updated-node)))

(defn graph-to-viz [nodes]
  (str "digraph G {\n"
       (apply str
              (for [node nodes child (node :connections)]
                (str "\tn" (name (node :pk)) " -> n" (name child) "\n")))
       "}"))

(defn avg [a]
  (/
   (apply + a)
   (count a)))

(defn compute-stats [nodes]
  ; outlinks [avg min max]
  ; inlinks [avg min max]
  ; span [avg min max]
  (let [connection-counts (map #(count (% :connections)) nodes)
        inlinks (apply concat (map :connections nodes))
        inlink-counts (map (fn [node] (count (filter #(= % (node :pk)) inlinks))) nodes)]
    {:count (count nodes)
     :outlinks [(avg connection-counts)
                (apply min connection-counts)
                (apply max connection-counts)]
     :inlinks [(avg inlink-counts)
               (apply min inlink-counts)
               (apply max inlink-counts)]}))

(defn component-render-graph [nodes stats]
  [:div [:h2 "swarm connectivity"]
   (let [v (graph-to-viz nodes)]
     (print v)
     [:div
      [:pre (str stats)]
      ;[:div {:dangerouslySetInnerHTML {:__html (viz v #js {"format" "svg" "engine" "fdp"})}}]
      ;[:pre v]
      [:span "end"]])])

(defn make-nodes [n]
  (loop [remaining n nodes []]
    (if (> remaining 0)
      (let [new-node (make-node remaining)
            introducer-nodes (vec (take introducer-peer-count nodes))
            introducer-node-peer-pks (set (apply concat (map :connections introducer-nodes)))
            introducer-node-peers (concat introducer-nodes (filter #(contains? introducer-node-peer-pks (% :pk)) nodes))
            new-node (select-initial-nodes new-node introducer-node-peers)
            nodes (introduce-new-node nodes new-node)]
        (recur
          (dec remaining)
          (concat nodes [new-node])))
      nodes)))

;; -------------------------
;; Initialize app

(defn mount-root []
  (let [nodes (make-nodes node-count)
        stats (compute-stats nodes)]
    (print "stats:" stats)
    (r/render [component-render-graph nodes stats] (.getElementById js/document "app"))))

(defn init! []
  (mount-root))
