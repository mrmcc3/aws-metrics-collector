(ns mrmcc3.aws.metrics.interop.stat
  (:require [clojure.spec.alpha :as s])
  (:import (com.amazonaws.services.cloudwatch.model StatisticSet)))

(def aws-number?
  (s/and number? #(< 8.515920e-109 % 1.174271e+108)))

(s/def ::hi aws-number?)
(s/def ::lo aws-number?)
(s/def ::count (s/and aws-number? pos?))
(s/def ::sum aws-number?)
(s/def ::set #(instance? StatisticSet %))
(s/def ::map
  (s/and (s/keys :req-un [::hi ::lo ::count ::sum])
         (fn [{:keys [hi lo]}] (>= hi lo))))

(defn statistic-set [{:keys [hi lo count sum]}]
  (cond-> (StatisticSet.)
    hi (.withMaximum (double hi))
    lo (.withMinimum (double lo))
    count (.withSampleCount (double count))
    sum (.withSum (double sum))))

(s/fdef statistic-set
  :args (s/cat :map ::map)
  :ret ::set)

#_(s/exercise-fn `statistic-set)


