## sqsc

Lightweight Clojure library for AWS SQS

### Deps

```clojure
omnyway-labs/sqsc
{:git/url "https://github.com/omnyway-labs/sqsc.git"
 :sha "918ddcda95cd4307d577a0584bab6e22fb642f40"}
```

### Usage

```clojure
(require '[sqsc.core :as sqs])

(sqs/init! {:provider :profile
            :profile (System/getenv "AWS_PROFILE")})
 

(sqs/create-queue "my-test-queue")
;; https://sqs.us-east...

(def url (sqs/get-queue-url "my-test-queue"))
;; https://sqs.us-east...

(sqs/send url "foo")
;; {:message-id "id123" ...}

(def msg (first (sqs/receive url)))

(:body msg)
;; "foo"

(sqs/delete url (:receipt-handle msg))

(sqs/delete-queue url)
```

### License

Copyright 2019 Omnyway Inc.

Licensed under the Apache License, Version 2.0 (the “License”); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
