(ns wrestler.handlers
  "A module for clj-http response handlers"
  (:require [clojure.data.json :as json]))

(def http-codes
  "A mapping of HTTP code numbers to human-readable keywords."
  {100 :continue
   101 :switching-protocols
   102 :webdav-processing
   200 :ok
   201 :created
   202 :accepted
   203 :non-authorative-information
   204 :no-content
   205 :reset-content
   206 :partial-content
   207 :webdav-multi-status
   208 :webdav-already-reported
   226 :im-used
   300 :multiple-choices
   301 :moved-permanentely
   302 :found
   303 :see-other
   304 :not-modified
   305 :use-proxy
   306 :switch-proxy
   307 :temporary-redirect
   308 :permanent-redirect
   400 :bad-request
   401 :unauthorized
   402 :paymend-required
   403 :forbidden
   404 :not-found
   405 :method-not-allowed
   406 :not-acceptable
   407 :proxy-authentification-required
   408 :request-timeout
   409 :conflict
   410 :gone
   411 :length-required
   412 :precondition-failed
   413 :request-entity-too-large
   414 :request-uri-too-long
   415 :unsupported-media-type
   416 :requested-range-not-satisfiable
   417 :expectation-failed
   418 :i-am-a-teapot; yay!
   419 :authentification-timeout
   422 :webdav-unprocessable-entity
   423 :webdav-locked
   424 :webdave-failed-dependency
   426 :upgrade-required
   428 :precondition-required
   429 :too-many-requests
   431 :request-header-fields-too-large
   440 :login-timeout
   444 :no-response
   449 :retry
   450 :blocked-by-windows-parental-controls
   451 :unavailable-for-legal-reasons 
   494 :request-header-too-large
   495 :certificate-error
   496 :no-certificate
   497 :http-to-https
   498 :token-invalid
   499 :token-required
   500 :internal-server-error
   501 :not-implemented
   502 :bad-gateway
   503 :service-unvailable
   504 :gateway-timeout
   505 :http-version-not-supported
   506 :variant-also-negotiates
   507 :webdav-insufficient-storage
   508 :webdav-loop-detected
   509 :bandwith-limit-exceeded
   510 :not-extended
   511 :network-authentification-required
   520 :origin-error
   521 :web-server-is-down
   522 :connection-timed-out
   523 :proxy-declined-request
   524 :a-timeout-occured
   598 :network-read-timeout-error
   599 :network-connect-timeout-error})


(defn to-edn
  "A response handler for clj-http responses. In HTTP, every response that
  returns with a status code between 200 and 299 is considered a success. If a
  response is received that doesn't have a status code in that range a message
  will be printed to STDERR. But before that happens the clj-http library will
  probably throw an exception anyway and prevent this handler from being called
  in the first place.
 
  If the response hash-map contains a :body key that does not refer to nil this
  response handler will try to convert the response to clojure data (EDN). If
  the server sends a Content-Type header that contains the specification
  application/json the data will be parsed with the clojure.data.json library to
  get clojure data.  Otherwise you will probably get a plain string. Further
  parsing of response types (XML etc.) might be implemented in the future.

  If the response hash-map doesn't contain a :body key or if it refers to nil
  then the complete response hash-map is returned unless the status code is a
  key in the hash-map wrestler.handlers.http-codes . In that case the
  coresponding value from the hash-map is returned. If this interferes with your
  debugging process consider (reset!)ing your response-handler to something
  different while debugging."
  [response]
  (let [code (:status response)
        body (:body response)]
    (when (not (<= 200 code 299))
      (binding [*out* *err*]
        (println "[ERROR] Received a " code " HTTP status code response")))
    (if (nil? body); if not content has been sent from the server
        (if-let [code-name (http-codes code)]
          code-name; return name of the status code
          response); return unchanged response
         (let [content-type (.toLowerCase ((:headers response) "Content-Type"))]
           (if (= -1 (.indexOf content-type "application/json"))
               body; no JSON. Return as-is.
               (json/read-str body))))))
