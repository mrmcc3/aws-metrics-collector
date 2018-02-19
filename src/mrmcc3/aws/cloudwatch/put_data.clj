(ns mrmcc3.aws.cloudwatch.put-data
  (:require
    [clojure.spec.alpha :as s]
    [mrmcc3.aws.cloudwatch.metric :as metric])
  (:import
    (com.amazonaws.services.cloudwatch.model PutMetricDataRequest)))

;; specs

(s/def ::namespace string?)
(s/def ::datums (s/coll-of ::metric/datum :min-count 1 :max-count 20))
(s/def ::map (s/keys :req-un [::namespace ::datums]))
(s/def ::request #(instance? PutMetricDataRequest %))

;; aws sdk interop

(defn request [{:keys [namespace datums]}]
  (-> (PutMetricDataRequest.)
      (.withNamespace namespace)
      (.withMetricData datums)))

(s/fdef request :args (s/cat :put-data ::map) :ret ::request)

;; dev only

(comment


  (metric/data->datums
    (ffirst (s/exercise (s/coll-of ::metric/data :min-count 1 :max-count 20) 1)))

  (request {:namespace "Test" :datums *1})

  )
