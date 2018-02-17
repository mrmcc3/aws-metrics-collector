(ns mrmcc3.aws.cloudwatch.put-data
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [mrmcc3.aws.cloudwatch.metric :as metric])
  (:import
    (com.amazonaws.services.cloudwatch.model PutMetricDataRequest)))

(defn request [{:keys [namespace metrics]}]
  (-> (PutMetricDataRequest.)
      (.withNamespace namespace)
      (.withMetricData metrics)))

(s/def ::namespace string?)
(s/def ::metrics (s/coll-of ::metric/datum :min-count 1 :max-count 20))
(s/def ::map (s/keys :req-un [::namespace ::metrics]))

(s/def ::request
  (s/with-gen
    #(instance? PutMetricDataRequest %)
    #(gen/fmap request (s/gen ::map))))

(s/fdef request :args (s/cat :put-data ::map) :ret ::request)
