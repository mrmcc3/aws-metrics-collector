(ns mrmcc3.aws.cloudwatch.metric
  (:require
    [clojure.spec.alpha :as s]
    [mrmcc3.aws.cloudwatch.stat :as stat])
  (:import
    (com.amazonaws.services.cloudwatch.model Dimension MetricDatum)
    (java.util Date)))

;; specs

(s/def ::dimensions (s/map-of string? string? :max-count 10))
(s/def ::name (s/or :str string? :kw keyword?))
(s/def ::stats ::stat/map)
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

(s/def ::pair
  (s/cat :name ::name :data (s/or :value ::value :stats ::stats)))

(s/def ::data (s/or :map ::map :pair ::pair))

(s/def ::datum #(instance? MetricDatum %))

;; aws sdk interop. metric data -> MetricDatum transformation

(defn dimension [[name value]]
  (-> (Dimension.) (.withName name) (.withValue value)))

(defn name->str [[type val]]
  (case type :kw (subs (str val) 1) val))

(defn datum [{:keys [dimensions name stats resolution value timestamp unit]}]
  (cond-> (MetricDatum.)
    dimensions (.withDimensions (map dimension dimensions))
    name (.withMetricName (name->str name))
    stats (.withStatisticValues (stat/statistic-set stats))
    resolution (.withStorageResolution (int resolution))
    value (.withValue (double value))
    true (.withTimestamp (or timestamp (Date.)))
    unit (.withUnit unit)))

(defmulti conformed-data->datum #(case % ::s/invalid :bad-data (first %)))

(defmethod conformed-data->datum :pair [[_ {name :name [kw val] :data}]]
  (datum {:name name kw val}))

(defmethod conformed-data->datum :map [[_ input]]
  (datum input))

(defmethod conformed-data->datum :default [_]
  ;; log bad data warning?
  :bad-data)

(def datum-xform
  (comp
    (map (partial s/conform ::data))
    (map conformed-data->datum)
    (remove #{:bad-data})))

(defn data->datums [data]
  (into [] datum-xform data))

(s/fdef data->datums
  :args (s/cat :data (s/coll-of ::data))
  :ret (s/coll-of ::datum)
  :fn (fn [{:keys [data]} res] (= (count data) (count res))))

;; dev only

(comment

  (s/exercise ::map 1)
  (s/exercise ::pair 1)
  (s/exercise ::data 1)

  (s/exercise-fn `data->datums)

  (into [] datum-xform {:ResponseTime 1.2
                        :a.b/Requests {:hi 1 :lo 1 :sum 1 :count 1}})

  )

