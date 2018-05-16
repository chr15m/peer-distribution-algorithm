(ns node-graph-algorithm.prod
  (:require
    [node-graph-algorithm.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
