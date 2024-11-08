(ns vscode-extension-template.when-contexts
  (:require ["vscode" :as vscode]))

(defn set-context!+ [!state k v]
  (swap! !state assoc-in [:extension/when-contexts k] v)
  (vscode/commands.executeCommand "setContext" (str k) v))

(defn get-context [state k]
  (get-in state [:extension/when-contexts k]))

(comment
  (require '[vscode-extension-template.db :as db])
  (get-context db/!app-db :vscode-extension-template/isActive))