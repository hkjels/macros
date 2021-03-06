(ns workflo.macros.registry
  (require [clojure.spec :as s]
           [inflections.core :refer [plural]]))

(defn throw-registry-error
  "Throws an error generated by a registry."
  [msg]
  (throw (Exception. msg)))

(s/fdef defregistry*
  :args (s/cat :name symbol?)
  :ret  ::s/any)

(defn defregistry*
  "Defines a registry with the given name. The resulting registry
   maps names (e.g. screen, view or command names) to definitions
   (e.g. the definition of a screen, a command or a view).

   Definitions in the registry can be looked up using one of the
   utility functions that are defined implicitly.

   (defregistry* 'command)

   will result in the following functions to be defined:

   (defn register-command! [name def] ...)
   (defn unregister-command! [name] ...)
   (defn registered-commands [] ...)
   (defn reset-registered-commands! [] ...)
   (defn resolve-command [name] ...)."
  ([name]
   (defregistry* name nil))
  ([name env]
   (let [registry-sym   (symbol (str "+" name "-registry+"))
         register-sym   (symbol (str "register-" name "!"))
         unregister-sym (symbol (str "unregister-" name "!"))
         registered-sym (symbol (str "registered-" (plural name)))
         reset-sym      (symbol (str "reset-registered-"
                                     (plural name) "!"))
         resolve-sym    (symbol (str "resolve-" name))]
     `(do
        (defonce ^:private ~registry-sym (atom (sorted-map)))
        (defn ~register-sym
          [~'name ~'def]
          (swap! ~registry-sym assoc ~'name ~'def))
        (defn ~unregister-sym
          [~'name]
          (swap! ~registry-sym dissoc ~'name))
        (defn ~registered-sym
          []
          (deref ~registry-sym))
        (defn ~reset-sym
          []
          (reset! ~registry-sym (sorted-map)))
        (defn ~resolve-sym
          [~'name]
          (let [~'definition (get (~registered-sym) ~'name)]
            (when (nil? ~'definition)
              (let [~'msg (str "Failed to resolve " ~(str name)
                               " '" ~'name "'")]
                (throw-registry-error ~'msg)))
            ~'definition))))))

(defmacro defregistry
  "Defines a registry with the given name. See defregistry* for
   more information."
  [name]
  (defregistry* name &env))
