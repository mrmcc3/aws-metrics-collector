(ns mrmcc3.aws.cloudwatch.collector
  (:require
    [clojure.spec.alpha :as s]
    [clojure.core.async :as a]
    [mrmcc3.aws.cloudwatch.put-data :as put-data]
    [mrmcc3.aws.cloudwatch.metric :as metric])
  (:import
    (com.amazonaws.services.cloudwatch
      AmazonCloudWatchAsyncClient AmazonCloudWatchAsyncClientBuilder)))

(s/def ::batch-size (s/and int? #(<= 1 % 20)))
(s/def ::batch-time (s/and int? #(>= % 1000)))
(s/def ::buffer-size pos-int?)
(s/def ::namespace string?)
(s/def ::opts (s/keys :req-un [::namespace]
                      :opt-un [::batch-size ::batch-time ::buffer-size]))

(defn metric-chan
  ([opts] (metric-chan (AmazonCloudWatchAsyncClientBuilder/defaultClient) opts))
  ([client {:keys [namespace batch-size batch-time buffer-size]
            :or   {batch-size 20 batch-time 60000 buffer-size 1000}
            :as   opts}]
   {:pre [(s/valid? ::opts opts)]}
   (let [input-ch (a/chan (a/dropping-buffer buffer-size)
                          metric/datum-xform
                          (constantly ::bad-datum))
         batch-ch (a/chan (a/buffer 1)
                          (map put-data/request)
                          (constantly ::bad-request))
         timeout-fn #(a/timeout batch-time)]

     ;; batching
     (a/go-loop [batch [] timeout (timeout-fn)]
       (if (>= (count batch) batch-size)
         (do (a/>! batch-ch {:datums batch :namespace namespace})
             (recur [] (timeout-fn)))
         (let [[metric ch] (a/alts! [input-ch timeout])]
           (if (= ch input-ch)
             (case metric
               nil (a/close! batch-ch)
               ::bad-datum (recur batch timeout)
               (recur (conj batch metric) timeout))
             (do (when (seq batch)
                   (a/>! batch-ch {:datums batch :namespace namespace}))
                 (recur [] (timeout-fn)))))))

     ;; requests
     (a/go-loop []
       (when-let [req (a/<! batch-ch)]
         (when-not (= ::bad-request req)
           (.putMetricDataAsync client req))
         (recur)))

     input-ch)))

;; dev only

(comment

  (def ch (metric-chan {:namespace "Test" :batch-time 1000}))

  (a/onto-chan
    ch
    {:ResponseTime 150
     :Requests {:hi 10 :lo 5 :sum 100 :count 200}}
    false)

  (a/close! ch)

  )