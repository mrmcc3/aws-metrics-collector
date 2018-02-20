(ns mrmcc3.aws.metrics.interop.req
  (:require
    [clojure.spec.alpha :as s]
    [mrmcc3.aws.metrics.interop.metric :as metric])
  (:import
    (com.amazonaws.services.cloudwatch.model PutMetricDataRequest)))

(s/def ::namespace string?)
(s/def ::datums (s/coll-of ::metric/datum :min-count 1 :max-count 20))
(s/def ::map (s/keys :req-un [::namespace ::datums]))
(s/def ::request #(instance? PutMetricDataRequest %))

(defn put-metric-data [{:keys [namespace datums]}]
  (-> (PutMetricDataRequest.)
      (.withNamespace namespace)
      (.withMetricData datums)))

(s/fdef put-metric-data :args (s/cat :put-data ::map) :ret ::request)

#_(s/exercise-fn `put-metric-data)
