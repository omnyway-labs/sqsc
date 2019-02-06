(ns sqsc.core
  (:require [sqsc.client :as client]
            [clojure.walk :refer [stringify-keys]]
            [clj-uuid :as uuid])
  (:import [com.amazonaws.services.sqs.model
            ListQueuesRequest
            GetQueueUrlRequest
            CreateQueueRequest
            DeleteQueueRequest
            ReceiveMessageRequest
            DeleteMessageRequest
            DeleteMessageBatchRequest
            DeleteMessageBatchRequestEntry
            SendMessageRequest
            SendMessageBatchRequest
            SendMessageBatchRequestEntry])
  (:refer-clojure :exclude [send]))

(defn as-batch-result [x]
  {:failed            (mapv (fn [failure]
                              {:code          (.getCode failure)
                               :id            (.getId failure)
                               :message       (.getMessage failure)
                               :sender-fault? (.isSenderFault failure)})
                            (.getFailed x))
   :successful        (mapv #(.getId %) (.getSuccessful x))
   :batch-successful? (empty? (.getFailed x))})

(defn as-message [m]
  {:body (.getBody m)
   :message-id (.getMessageId m)
   :receipt-handle (.getReceiptHandle m)
   :attributes (.getAttributes m)})

(defn as-messages [xs]
  (map as-message xs))

(defn list-queues []
  (-> (client/lookup)
      .listQueues
      .getQueueUrls))

(defn get-queue-url [queue-name]
  (-> (client/lookup)
      (.getQueueUrl queue-name)
      .getQueueUrl))

(defn create-queue [name & [attributes]]
  (let [request (cond-> (CreateQueueRequest. name)
                  (seq attributes) (.withAttributes (stringify-keys attributes)))]
    (-> (client/lookup)
        (.createQueue request)
        .getQueueUrl)))

(defn delete-queue [queue-url]
  (-> (client/lookup)
      (.deleteQueue queue-url)))

(defn receive [queue-url & [{:keys [wait-time max-messages visibility-timeout
                                    attribute-names receive-attempt-id]}]]
  (let [request (cond-> (ReceiveMessageRequest. queue-url)
                  wait-time (.withWaitTimeSeconds (int wait-time))
                  max-messages (.withMaxNumberOfMessages (int max-messages))
                  visibility-timeout (.withVisibilityTimeout (int visibility-timeout))
                  (seq attribute-names) (.withAttributeNames (map name attribute-names))
                  receive-attempt-id (.withReceiveRequestAttemptId receive-attempt-id))]
    (-> (client/lookup)
        (.receiveMessage request)
        .getMessages
        as-messages)))

(defn delete [queue-url receipt-handle]
  (-> (client/lookup)
      (.deleteMessage queue-url receipt-handle)))

(defn delete-batch [queue-url receipt-handles]
  (let [entries (mapv (fn [receipt-handle]
                        (DeleteMessageBatchRequestEntry. (str (uuid/v4)) receipt-handle))
                      receipt-handles)]
    (-> (client/lookup)
        (.deleteMessageBatch queue-url entries)
        as-batch-result)))

(defn send [queue-url message & [{:keys [delay deduplication-id group-id]}]]
  (let [request (cond-> (SendMessageRequest. queue-url message)
                  delay (.withDelaySeconds (int delay))
                  deduplication-id (.withMessageDeduplicationId deduplication-id)
                  group-id (.withMessageGroupId group-id))]
    (-> (client/lookup)
        (.sendMessage request)
        ((fn [x]
           {:message-id (.getMessageId x)
            :sequence-number (.getSequenceNumber x)})))))

(defn send-batch [queue-url messages]
  (let [entries (mapv (fn [{:keys [id body delay deduplication-id group-id]}]
                        (cond-> (SendMessageBatchRequestEntry. (or id (str (uuid/v4))) body)
                          delay (.withDelaySeconds (int delay))
                          deduplication-id (.withMessageDeduplicationId deduplication-id)
                          group-id (.withMessageGroupId group-id)))
                      messages)]
    (-> (client/lookup)
        (.sendMessageBatch queue-url entries)
        as-batch-result)))

(defn init! [auth]
  (client/init! auth))
