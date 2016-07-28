(ns workflo.macros.command
  (:require [clojure.spec :as s]
            [workflo.macros.command.util :as util]
            [workflo.macros.query :as q]
            [workflo.macros.config :refer [defconfig]]
            [workflo.macros.registry :refer [defregistry]]
            [workflo.macros.specs.command]
            [workflo.macros.util.form :as f]
            [workflo.macros.util.symbol :refer [unqualify]]))

;;;; Configuration options for the defcommand macro

(defconfig command
  ;; Supports the following options:
  ;;
  ;; :query - a function that takes a parsed query; this function
  ;;          is used to query a cache for data that the command
  ;;          being executed needs to run.
  ;;
  ;; :process-emit - a function that is called after a command has
  ;;                 been executed; it takes the data returned from
  ;;                 the command emit function and handles it in
  ;;                 whatever way is desirable.
  {:query nil
   :process-emit nil})

;;;; Command registry

(defregistry command)

;;;; Command execution

(defn run-command!
  [cmd-name data]
  (let [definition (resolve-command cmd-name)]
    (when (:data-spec definition)
      (assert (s/valid? (:data-spec definition) data)
              (str "Command data is invalid:"
                   (s/explain-str (:data-spec definition) data))))
    (let [query          (some-> definition :query
                                 (q/bind-query-parameters data))
          query-result   (when query
                           (some-> (get-command-config :query)
                                   (apply [query])))
          command-result ((:emit definition) query-result data)]
      (if (get-command-config :process-emit)
        (-> (get-command-config :process-emit)
            (apply [command-result]))
        command-result))))

;;;; The defcommand macro

(s/fdef defcommand*
  :args :workflo.macros.specs.command/defcommand-args
  :ret  ::s/any)

(defn defcommand*
  ([name forms]
   (defcommand* name forms nil))
  ([name forms env]
   (let [args-spec   :workflo.macros.specs.command/defcommand-args
         args        (if (s/valid? args-spec [name forms])
                       (s/conform args-spec [name forms])
                       (throw (Exception.
                               (s/explain-str args-spec
                                              [name forms]))))
         description (:description (:forms args))
         query       (some-> args :forms :query :form-body q/parse)
         query-keys  (some-> query q/map-destructuring-keys)
         data-spec   (some-> args :forms :data-spec :form-body)
         name-sym    (unqualify name)
         forms       (cond-> (:forms (:forms args))
                       true        (conj (:emit (:forms args)))
                       description (conj {:form-name 'description})
                       query       (conj {:form-name 'query})
                       data-spec   (conj {:form-name 'data-spec}))
         form-fns    (->> forms
                          (remove (comp nil? :form-body))
                          (map #(update % :form-name
                                        f/prefixed-form-name
                                        name-sym))
                          (map #(assoc % :form-args
                                       '[query-result data]))
                          (map #(cond-> %
                                  query-keys (update :form-body
                                                     util/bind-query-keys
                                                     query-keys)))
                          (map f/form->defn))
         def-sym     (f/qualified-form-name 'definition name-sym)]
     `(do
        ~@form-fns
        ~@(when description
            `(~(f/make-def name-sym 'description description)))
        ~@(when query
            `((~'def ~(f/prefixed-form-name 'query name-sym)
               '~query)))
        ~@(when data-spec
            `(~(f/make-def name-sym 'data-spec data-spec)))
        ~(f/make-def name-sym 'definition
           (f/forms-map forms name-sym))
        (register-command! '~name ~def-sym)))))

(defmacro defcommand
  [name & forms]
  (defcommand* name forms &env))
