(ns sqsc.client
  (:require [saw.core :as saw])
  (:import [com.amazonaws.services.sqs
            AmazonSQSClientBuilder]))

(defonce client (atom nil))

(defn lookup [] @client)

(defn make [region]
  (-> (AmazonSQSClientBuilder/standard)
      (.withCredentials (saw/creds))
      (.withRegion region)
      .build))

(defn init! [{:keys [region] :as auth}]
  (saw/login auth)
  (reset! client (make region)))
