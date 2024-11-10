(ns vsc-et.extension.db
  (:require ["vscode" :as vscode]))

(def init-db
  {::command-queue []
   ::cleanup {}
   ::context nil
   :workspace/root-path nil
   :extension/when-contexts {:vsc-et/active? false}})

(defonce !app-db (atom init-db))

(comment
  @!app-db
  :rcf)

(defn cleanup! []
  (doseq [[id ^js cleanup] (::cleanup @!app-db)]
    (.dispose cleanup)
    (swap! !app-db update ::cleanup dissoc id)))

(defn register-command! [id command-fn]
  (swap! !app-db update [::command-queue] conj {:id id :command-fn command-fn}))

(defn set-context!+ [k v]
  (swap! !app-db assoc-in [:extension/when-contexts k] v)
  (vscode/commands.executeCommand "setContext" (str k) v))

(defn get-vscode-context! ^js []
  (::context @!app-db))

(defn get-context [k]
  (get-in @!app-db [:extension/when-contexts k]))

(defn- refresh-commands! []
  (try
    (doseq [{:keys [id command-fn]} (::command-queue @!app-db)]
      ;; if id is already registered, dispose of it properly
      (when-some [^js cleanup (get-in @!app-db [::cleanup id])]
        (.dispose cleanup)

        ;; just in case a command id ever changes, otherwise not necessary since next step overrides
        (swap! !app-db update ::cleanup dissoc id))

      ;; register and remember disposable in the first place
      (let [cleanup (vscode/commands.registerCommand id command-fn)]
        (swap! !app-db assoc-in [::cleanup id] cleanup)))

    (finally
      (swap! !app-db assoc ::command-queue []))))

(defn activate! [context]
  (swap! !app-db assoc
    ::context context
    :workspace/root-path (some-> vscode/workspace.workspaceFolders first))

  (refresh-commands!))

(defn ^:dev/after-load reload! []
  (refresh-commands!))