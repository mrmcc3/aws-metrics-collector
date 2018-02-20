(ns mrmcc3.aws.metrics.interop.metric
  (:require
    [clojure.spec.alpha :as s]
    [mrmcc3.aws.metrics.interop.stat :as stat]
    [clojure.spec.gen.alpha :as gen])
  (:import
    (com.amazonaws.services.cloudwatch.model Dimension MetricDatum)
    (java.util Date)))

(s/def ::dimensions (s/map-of string? string? :max-count 10))
(s/def ::stats ::stat/map)
(s/def ::name string?)
(s/def ::resolution #{1 60})
(s/def ::timestamp inst?)
(s/def ::value stat/aws-number?)
(s/def ::unit
  #{"Seconds" "Microseconds" "Milliseconds" "Bytes" "Kilobytes"
    "Megabytes" "Gigabytes" "Terabytes" "Bits" "Kilobits" "Megabits"
    "Gigabits" "Terabits" "Percent" "Count" "Bytes/Second"
    "Kilobytes/Second" "Megabytes/Second" "Gigabytes/Second"
    "Terabytes/Second" "Bits/Second" "Kilobits/Second" "Megabits/Second"
    "Gigabits/Second" "Terabits/Second" "Count/Second" "None"})

(s/def ::map
  (s/keys :req-un [::name (or ::value ::stats)]
          :opt-un [::dimensions ::resolution ::timestamp ::unit]))

(defn dimension [[name value]]
  (-> (Dimension.) (.withName name) (.withValue value)))

(defn datum [{:keys [dimensions name stats resolution value timestamp unit]}]
  (cond-> (MetricDatum.)
    dimensions (.withDimensions (map dimension dimensions))
    name (.withMetricName (str name))
    stats (.withStatisticValues (stat/statistic-set stats))
    resolution (.withStorageResolution (int resolution))
    value (.withValue (double value))
    true (.withTimestamp (or timestamp (Date.)))
    unit (.withUnit unit)))

(s/def ::datum
  (s/with-gen
    #(instance? MetricDatum %)
    #(gen/fmap datum (s/gen ::map))))

(s/fdef datum
  :args (s/cat :map ::map)
  :ret ::datum)

#_(s/exercise-fn `datum)
