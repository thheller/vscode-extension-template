(ns vsc-et.extension
  (:require [vsc-et.extension.db :as db]))

;;;;; Extension activation entry point

(defn ^:export activate [context]
  (db/activate! context)

  ;; do not know what this is?
  (db/set-context!+ :vsc-et/active? true)

  ;; return API
  #js {:v1 {}})

(defn ^:export deactivate []
  (db/set-context!+ :vsc-et/active? false)
  (db/cleanup!))