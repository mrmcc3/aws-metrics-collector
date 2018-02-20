(ns mrmcc3.aws.metrics.collector
  (:require
    [clojure.core.async :as a]
    [clojure.core.async.impl.protocols :refer [Buffer]]
    [clojure.spec.alpha :as s]
    [mrmcc3.aws.metrics.transform :as transform]
    [mrmcc3.aws.metrics.interop.req :as req])
  (:import
    (com.amazonaws.services.cloudwatch
      AmazonCloudWatchAsyncClient AmazonCloudWatchAsyncClientBuilder)))

(s/def ::batch-size (s/and int? #(<= 1 % 20)))
(s/def ::timeout (s/and int? #(>= % 1000)))
(s/def ::buffer #(satisfies? Buffer %))
(s/def ::namespace string?)
(s/def ::opts (s/keys :opt-un [::namespace ::batch-size ::timeout ::buffer]))

(def defaults
  {:namespace  "aws-metrics-collector"
   :buffer     (a/dropping-buffer 1000)
   :batch-size 20
   :timeout    60000})

(defn collector
  "Returns a channel that accepts metric data, transforms it to
  AWS MetricDatums, batches them, builds AWS PutMetricDataRequests
  and periodically sends them to AWS CloudWatch using the provided
  client (or the defaultClient if not provided). Non-conforming
  metric data will be dropped. opts are

  :namespace - AWS Cloudwatch Metrics Namespace
  :batch-size - number of datums to batch before sending
  :timeout - time to wait before sending partial batch
  :buffer - a core.async buffer to use on the input channel"
  ([] (collector {}))
  ([opts] (collector (AmazonCloudWatchAsyncClientBuilder/defaultClient) opts))
  ([async-client opts]
   {:pre [(s/valid? ::opts opts)]}
   (let [{:keys [namespace batch-size timeout buffer]}
         (merge defaults opts)
         input-ch   (a/chan buffer transform/data->datums)
         batch-ch   (a/chan 1 (map req/put-metric-data))
         timeout-fn #(a/timeout timeout)]

     ;; batching
     (a/go-loop [batch [] timeout (timeout-fn)]
       (if (>= (count batch) batch-size)
         (do (a/>! batch-ch {:datums batch :namespace namespace})
             (recur [] (timeout-fn)))
         (let [[metric ch] (a/alts! [input-ch timeout])]
           (if (= ch input-ch)
             (if metric
               (recur (conj batch metric) timeout)
               (a/close! batch-ch))
             (do (when (seq batch)
                   (a/>! batch-ch {:datums batch :namespace namespace}))
                 (recur [] (timeout-fn)))))))

     ;; requests
     (a/go-loop []
       (when-let [req (a/<! batch-ch)]
         (.putMetricDataAsync async-client req)
         (recur)))

     input-ch)))


(comment

  (def ch (collector {:namespace "Test"
                      :timeout 5000
                      :buffer (a/sliding-buffer 10)
                      :batch-size 5}))
  (a/put! ch {:name "ResponseTime" :value 101})
  (a/onto-chan ch {:ResponseTime 100} false)
  (a/close! ch)

  )
