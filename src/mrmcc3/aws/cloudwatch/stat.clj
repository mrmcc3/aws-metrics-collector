(ns mrmcc3.aws.cloudwatch.stat
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen])
  (:import
    (com.amazonaws.services.cloudwatch.model StatisticSet)))

;; specs

(def aws-number? (s/and number? #(< 8.515920e-109 % 1.174271e+108))) ;; see https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/cloudwatch/model/MetricDatum.html#setValue-java.lang.Double-

(s/def ::hi aws-number?)
(s/def ::lo aws-number?)
(s/def ::count (s/and aws-number? pos?))
(s/def ::sum aws-number?)

(s/def ::map
  (s/and (s/keys :req-un [::hi ::lo ::count ::sum])
         (fn [{:keys [hi lo]}] (>= hi lo))))

(s/def ::set
  (s/with-gen #(instance? StatisticSet %)
              #(gen/fmap statistic-set (s/gen ::map))))

;; aws sdk interop

(defn statistic-set [{:keys [hi lo count sum]}]
  (cond-> (StatisticSet.)
    hi (.withMaximum (double hi))
    lo (.withMinimum (double lo))
    count (.withSampleCount (double count))
    sum (.withSum (double sum))))

(s/fdef statistic-set :args (s/cat :stat ::map) :ret ::set)

;; dev only

(comment

  (s/exercise ::map)
  (s/exercise ::set)
  (s/exercise-fn `statistic-set)

  )
