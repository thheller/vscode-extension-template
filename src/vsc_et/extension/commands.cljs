(ns vsc-et.extension.commands
  (:require
    ["vscode" :as vscode]
    [vsc-et.extension.db :as db]
    [promesa.core :as p]))

(defn hello!+ [s]
  (p/let [s (or s (vscode/window.showInputBox #js {:title "Hello Input"
                                                   :placeHolder "What should we say hello to today?"}))]
    (vscode/window.showInformationMessage (str "Hello, " s "!"))))

(db/register-command! "vsc-et.hello" hello!+)

(defn new-untitled-hello-document!+ [s]
  (p/let [s (or s (vscode/window.showInputBox #js {:title "Hello Input"
                                                   :placeHolder "What should we say hello to today?"}))
          document (vscode/workspace.openTextDocument #js {:content (str "Hello, " s "!")})]
    (vscode/window.showTextDocument document)))

(db/register-command! "vsc-et.newHelloDocument" new-untitled-hello-document!+)
