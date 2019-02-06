(ns sqsc.core-test
  (:require [sqsc.core :as sqs]
            [saw.core :as saw]
            [clojure.test :refer :all]
            [clj-uuid :as uuid]))

(defn sqs-fixture [f]
  (sqs/init! (saw/session))
  (f))

(use-fixtures :each sqs-fixture)

(deftest basic-sqs-test
  (testing "create queue / send, receive, delete messages / delete queue"
    (is (not-empty (sqs/list-queues)))
    (let [queue-name (str "sqs-test-" (str (uuid/v4)))
          url (sqs/create-queue queue-name)]
      (is (= url (sqs/get-queue-url queue-name)))
      (is (:message-id (sqs/send url "foo")))
      (let [message (first (sqs/receive url))]
        (is (= (:body message) "foo"))
        (is (sqs/delete url (:receipt-handle message)))
        (is (empty? (sqs/receive url))))
      (is (:batch-successful? (sqs/send-batch url [{:body "foo"}
                                                   {:body "bar"}
                                                   {:body "baz"}])))
      (let [messages (loop [messages []]
                       (if (= (count messages) 3)
                         messages
                         (recur (concat messages (sqs/receive url {:wait-time 10})))))]
        (is (:batch-successful? (sqs/delete-batch url (map :receipt-handle messages)))))
      (is (sqs/delete-queue url)))))
