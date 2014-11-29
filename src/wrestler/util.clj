(ns wrestler.util
  (:import [java.io StringWriter])
  (:require [clj-http.client :as http])
  (:require [clojure.data.json :as json]))

(def rest-methods
  {:get http/get
   :post http/post
   :put http/put
   :del http/delete})

(defn to-json [data]
  (let [sw (StringWriter.)]
    (json/write data sw :escape-slash false); escaped slashes are problematic with URLs
    (str sw)))

(defn extract-url-params
  "Given an URL like foo/$bar/baz/$bang/boom/
  get all the $variables and return a list of symbols that contains these
  variables without the leading $"
  [url]
  (for [token (clojure.string/split url #"/") :when (= (first token) \$)]
    (-> token (.substring 1) (clojure.string/split #"\.") first symbol))); split by . to strip file extensions

(defn join-query-params
  "Convert a clojure hashmap to a ?key=value&key=value string"
  [params]
  (if params
    (str "?" (clojure.string/join
               "&"
               (for [[k v] params :when (not (:json k))]
                 (str (name k) "=" v))))
    ""))

(defn interpolate-url
  "Given an URL with $variables in it, consecutively replace every occurance of
  $variable with one of the params."
  [url params]
  (letfn [(interpolate-url-part [part value]; map VarName.json -> VarValue.json
            (let [[base & extensions] (clojure.string/split part #"\." 2)]
              (clojure.string/join "." (cons value extensions))))]
    (loop [interpolated-url-parts []
           url-tokens (clojure.string/split url #"/")
           params params]
      (if-let [url-next (first url-tokens)]
        (if
          (= (first url-next) \$);if we can replace a variable
          (recur
            (conj interpolated-url-parts (interpolate-url-part
                                           url-next
                                           (first params)))
            (next url-tokens);drop
            (next params))
          (recur
            (conj interpolated-url-parts url-next)
            (next url-tokens);take
            params));keep
        (clojure.string/join "/" interpolated-url-parts)))))

(defn request
  "Send a REST request to cytoscape"
  ([method url] (request method url nil))
  ([method url extra-params]
   (let [method (if (keyword? method) (method rest-methods) method)
         full-url (str url (join-query-params extra-params))
         json (:json extra-params)
         payload (if json
                    {:body (to-json json)
                     :content-type :json}
                    {})
         response (method full-url payload)]
     response)))
