(ns workflo.macros.registry-test
  (:require #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            #?(:cljs [workflo.macros.registry
                      :refer-macros [defregistry]]
               :clj  [workflo.macros.registry
                      :refer [defregistry]])))

(deftest defregistry-defines-all-expected-functions
  (defregistry view)
  (and (is (fn? register-view!))
       (is (fn? registered-views))
       (is (fn? reset-registered-views!))
       (is (fn? resolve-view))))

(deftest defregistry-works-as-expected
  (defregistry item)
  (register-item! 'foo :bar)
  (register-item! 'bar :baz)
  (and (is (= {'foo :bar 'bar :baz}
              (registered-items)))
       (is (= :bar (resolve-item 'foo)))
       (is (= :baz (resolve-item 'bar)))
       (do
         (reset-registered-items!)
         (is (= (sorted-map)
                (registered-items))))))
