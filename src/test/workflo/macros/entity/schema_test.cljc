(ns workflo.macros.entity.schema-test
  (:require #?(:cljs [cljs.spec :as s]
               :clj  [clojure.spec :as s])
            #?(:cljs [cljs.test :refer-macros [deftest is]]
               :clj  [clojure.test :refer [deftest is]])
            #?(:cljs [workflo.macros.entity
                      :refer [resolve-entity]
                      :refer-macros [defentity]]
               :clj  [workflo.macros.entity
                      :refer [resolve-entity
                              defentity]])
            [workflo.macros.entity.schema :as schema]
            [workflo.macros.specs.types]))

(defentity url/selected-user
  (spec :workflo.macros.specs.types/id))

(deftest entity-with-value-spec-type-id
  (let [entity (resolve-entity 'url/selected-user)]
    (and (is (not (nil? entity)))
         (is (= {:url/selected-user []}
                (schema/entity-schema entity))))))

(defentity ui/search-text
  (spec :workflo.macros.specs.types/string))

(deftest entity-with-value-spec-type-string
  (let [entity (resolve-entity 'ui/search-text)]
    (and (is (not (nil? entity)))
         (is (= {:ui/search-text [:string]}
                (schema/entity-schema entity))))))

(defentity ui/search-text-with-extended-spec
  (spec
   (s/and :workflo.macros.specs.types/string
          #(> (count %) 5))))

(s/def :db/id :workflo.macros.specs.types/id)
(s/def :user/email :workflo.macros.specs.types/string)
(s/def :user/name :workflo.macros.specs.types/string)
(s/def :user/bio :workflo.macros.specs.types/string)

(defentity user
  (spec
   (s/keys :req [:db/id :user/name :user/email]
           :opt [:user/bio])))

(deftest entity-with-keys-spec
  (let [entity (resolve-entity 'user)]
    (and (is (not (nil? entity)))
         (is (= {:db/id []
                 :user/email [:string]
                 :user/name [:string]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

(defentity user-with-extended-spec
  (spec
   (s/and (s/keys :req [:db/id :user/name :user/email]
                  :opt [:user/bio])
          #(> (count (:user/name %)) 5))))

(deftest entity-with-and-keys-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= {:db/id []
                 :user/email [:string]
                 :user/name [:string]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))

(deftest entity-with-and-value-spec
  (let [entity (resolve-entity 'user-with-extended-spec)]
    (and (is (not (nil? entity)))
         (is (= {:db/id []
                 :user/email [:string]
                 :user/name [:string]
                 :user/bio [:string]}
                (schema/entity-schema entity))))))
