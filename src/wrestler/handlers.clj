(ns wrestler.handlers
  (:require [clojure.data.json :as json]))

(defn to-edn
  "A response handler for clj-http responses. Tries to coerce the response to
  clojure data and will print a message if a non-200 or 204 status is received."
  [response]
  (if (= 200 (:status response))
    (case (.toLowerCase ((:headers response) "Content-Type"))
      "application/json" (json/read-str (:body response))
      "application/json; charset=utf-8" (json/read-str (:body response))
      (:body response))
    (if (= 204 (:status response))
        :no-content; HTTP 204 = no content but also no error
        (do (println "Error: Request failed. Returning complete response instead")
            response))))
