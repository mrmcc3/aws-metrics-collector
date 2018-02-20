(ns mrmcc3.aws.metrics.transform
  (:require
    [clojure.spec.alpha :as s]
    [mrmcc3.aws.metrics.interop.metric :as metric]))

(s/def ::key (s/or :str string? :kw keyword?))
(s/def ::val (s/or :value ::metric/value :stats ::metric/stats))
(s/def ::pair (s/cat :key ::key :val ::val))
(s/def ::data (s/or :map ::metric/map :pair ::pair))

(defn pair->metric [{[_ key] :key [kw val] :val}]
  {:name (str key) kw val})

(defn data->metric [[type data]] ;; multimethod?
  (case type
    :pair (pair->metric data)
    data))

(def data->datums
  (comp
    (map (partial s/conform ::data))
    (remove #{::s/invalid})
    (map data->metric)
    (map metric/datum)))

(defn datums [data]
  (into [] data->datums data))

(s/def ::input
  (s/or :coll (s/coll-of ::data)
        :map (s/map-of ::key ::val)))

(s/fdef datums
  :args (s/cat :input ::input)
  :ret (s/coll-of ::metric/datum)
  :fn (fn [{{:keys [input]} :args ret :ret}]
        (= (count (s/unform ::input input)) (count ret))))

#_(s/exercise-fn `datums)
