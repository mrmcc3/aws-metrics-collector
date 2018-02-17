(ns mrmcc3.aws.cloudwatch.metric
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [mrmcc3.aws.cloudwatch.stat :as stat])
  (:import
    (com.amazonaws.services.cloudwatch.model Dimension MetricDatum)))

(defn dimension [[name value]]
  (-> (Dimension.) (.withName name) (.withValue value)))

(defn datum
  [{:keys [dimensions name statistics resolution value timestamp unit]}]
  (cond-> (MetricDatum.)
    dimensions (.withDimensions (map dimension dimensions))
    name (.withMetricName name)
    statistics (.withStatisticValues (stat/statistic-set statistics))
    resolution (.withStorageResolution (int resolution))
    value (.withValue (double value))
    timestamp (.withTimestamp timestamp)
    unit (.withUnit unit)))

(s/def ::dimensions (s/map-of string? string? :max-count 10))
(s/def ::name string?)
(s/def ::statistics ::stat/map)
(s/def ::resolution #{1 60})
(s/def ::timestamp (s/inst-in #inst "2017" #inst "2018"))
(s/def ::value number?)

(s/def ::unit
  #{"Seconds" "Microseconds" "Milliseconds" "Bytes" "Kilobytes"
    "Megabytes" "Gigabytes" "Terabytes" "Bits" "Kilobits" "Megabits"
    "Gigabits" "Terabits" "Percent" "Count" "Bytes/Second"
    "Kilobytes/Second" "Megabytes/Second" "Gigabytes/Second"
    "Terabytes/Second" "Bits/Second" "Kilobits/Second" "Megabits/Second"
    "Gigabits/Second" "Terabits/Second" "Count/Second" "None"})

(s/def ::map
  (s/keys :req-un [::name ::value]
          :opt-un [::dimensions ::statistics ::resolution ::timestamp ::unit]))

(s/def ::datum
  (s/with-gen
    #(instance? MetricDatum %)
    #(gen/fmap datum (s/gen ::map))))

(s/fdef datum :args (s/cat :metric ::map) :ret ::datum)
