(ns e2e.test-runner
  (:require [clojure.string :as string]
            [cljs.test]
            [promesa.core :as p]
            [e2e.db :as db]
            ["vscode" :as vscode]
            workspace-activate))

(defn- write [& xs]
  (js/process.stdout.write (string/join " " xs)))

(defmethod cljs.test/report [:cljs.test/default :begin-test-var] [m]
  (write "===" (str (-> m :var meta :name) ": ")))

(defmethod cljs.test/report [:cljs.test/default :end-test-var] [m]
  (write " ===\n"))

(def old-pass (get-method cljs.test/report [:cljs.test/default :pass]))

(defmethod cljs.test/report [:cljs.test/default :pass] [m]
  (write "✅")
  (binding [*print-fn* write] (old-pass m))
  (swap! db/!state update :pass inc))

(def old-fail (get-method cljs.test/report [:cljs.test/default :fail]))

(defmethod cljs.test/report [:cljs.test/default :fail] [m]
  (write "❌")
  (binding [*print-fn* write] (old-fail m))
  (swap! db/!state update :fail inc))

(def old-error (get-method cljs.test/report [:cljs.test/default :error]))

(defmethod cljs.test/report [:cljs.test/default :error] [m]
  (write "🚫")
  (binding [*print-fn* write] (old-error m))
  (swap! db/!state update :error inc))

(def old-end-run-tests (get-method cljs.test/report [:cljs.test/default :end-run-tests]))

(defmethod cljs.test/report [:cljs.test/default :end-run-tests] [m]
  (binding [*print-fn* write]
    (old-end-run-tests m)
    (let [{:keys [running pass fail error]} @db/!state
          passed-minimum-threshold 2
          fail-reason (cond
                        (< 0 (+ fail error)) "FAILURE: Some tests failed or errored"
                        (< pass passed-minimum-threshold) (str "FAILURE: Less than " passed-minimum-threshold " assertions passed")
                        :else nil)]
      (println "Runner: tests run, results:" (select-keys  @db/!state [:pass :fail :error]))
      (if fail-reason
        (p/reject! running fail-reason)
        (p/resolve! running true)))))

(defn- file->ns [src-path file]
  (-> file
      (subs (inc (count src-path)))
      (string/replace #"\.cljs$" "")
      (string/replace #"/" ".")
      (string/replace #"_" "-")))

(defn- find-test-nss+ [src-path]
  (p/let [file-uris (vscode/workspace.findFiles (str src-path "/**/*_test.cljs"))
          files (.map file-uris (fn [uri]
                                  (vscode/workspace.asRelativePath uri false)))
          nss-strings (-> files
                          (.map (partial file->ns src-path))
                          sort)]
    (mapv symbol nss-strings)))

(defn- run-when-ws-activated [nss-syms tries]
  (if (:ws-activated? @db/!state)
    (-> (p/do
          (println "Running tests in" nss-syms)
          (apply require nss-syms)
          (apply cljs.test/run-tests nss-syms)
          (workspace-activate/clean-up!))
        (p/catch (fn [e]
                   (workspace-activate/clean-up!)
                   (p/reject! (:running @db/!state) e))))
    (do
      (println "Runner: Workspace not activated yet, tries: " tries "- trying again in a jiffy")
      (js/setTimeout #(run-when-ws-activated nss-syms (inc tries)) 10)
      (:running @db/!state))))

(defn run-all-tests [src-path]
  (let [running (p/deferred)]
    (swap! db/!state assoc :running running)
    (p/let [nss-syms (find-test-nss+ src-path)]
      (run-when-ws-activated nss-syms 1))
    running))

