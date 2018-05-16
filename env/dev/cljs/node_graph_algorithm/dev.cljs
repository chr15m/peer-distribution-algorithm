(ns ^:figwheel-no-load node-graph-algorithm.dev
  (:require
    [node-graph-algorithm.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)
