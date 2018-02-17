(ns mrmcc3.aws.cloudwatch.stat
  (:require [clojure.spec.alpha :as s])
  (:import (com.amazonaws.services.cloudwatch.model StatisticSet)))

(defn statistic-set [{:keys [hi lo count sum]}]
  (cond-> (StatisticSet.)
    hi (.withMaximum (double hi))
    lo (.withMinimum (double lo))
    count (.withSampleCount (double count))
    sum (.withSum (double sum))))

(s/def ::hi number?)
(s/def ::lo number?)
(s/def ::count number?)
(s/def ::sum number?)
(s/def ::map (s/and (s/keys :opt-un [::hi ::lo ::count ::sum])
                    #(not-empty %)))

(s/fdef statistic-set
  :args (s/cat :stat ::map)
  :ret #(instance? StatisticSet %))

