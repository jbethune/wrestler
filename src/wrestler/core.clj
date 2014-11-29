(ns wrestler.core
  "A library for defining REST wrappers for webservices.

  Defining a wrapper consists of the following steps:
  
  1. Define a default base-url and a default response handler as atoms
  2. Create a new wrapper factory with defREST and choose a name for it
  3. Define the individual wrappers with the wrapper factory
  4. Ship it to end users who can then (reset!) the base-url and the response
     handler if they need to.

  Examples:

  Step 1:
  
  Let us assume that every request to the REST api starts with the following
  URL:

  (def base-url (atom \"http://example.com/rest/\"))

  Then define a response handler for response objects returned by the clj-http
  library (https://github.com/dakrone/clj-http/):
  
  (def response-handler (atom
                          (fn [response]
                             (print \"Got an answer from the server!\")))

  Alternatively you can just use one of the handlers from wrestler.handlers but
  don't forget to call (atom) on them.
  
  Step 2:
  
  (defREST
    defMyAPI; This is the name of your new API wrapper
   'current-namespace/base-url
   'current-namespace/response-handler)

  Note the fully-qualified and quoted form of the base-url and response-handler
  atoms. These are required for the macros to work properly.

  Next, we will see how to use it.

  Step 3:
  
  Create the individual wrappers

  (defMyAPI get-database-size
  \"This is a mandatory docstring that describes the new function
  get-database-size\"
  :get \"subpath/that/is/appended/to/base-url/\")

  The newly created wrapper factory defMyAPI has the following parameters:
     1. Name of the wrapper function that will be created (get-database-size)
     2. Docstring of the wrapper function that will be created (\"This is a mandatory...\")
     3. HTTP method. Pick one of :get :post :put :del (:get)
     4. URL extension to the base URL. The base url and this URL will be glued
        together. This extension URL can also contain positional parameters
        defined with a leading $ (more on that later). (\"subpath/that/is/...\")
     5. (optional) Declaration of additional parameters (not present)

  You would call this function just like this:

  (get-database-size)

  Another example: This wrapper function expects two positional parameters.
  The first parameter is called \"name\" and the second positional parameter is
  a special parameter representing the trailing URL parameters. Such trailing
  URL parameters will be appended to the URL like this: ?answer=42&advice=dont_panic
  
  (defMyAPI create-new-database
  \"Write documentation here
  
  (url param) name: Name of the new database
  (query param) size: Initial size of the database
  \"
  :post \"create_database/$name\" :query)

  All normal positional parameters are defined with a $ followed by the name of
  the parameter. In this example the parameter is called \"name\" and written as
  $name. Parameter names expand up to the nearest \"/\" or \".\". You can have
  as many url parameters as you like: literal/$var1/$var2/literal/$var3.zip

  Then there are two other types of parameters: Query parameters and JSON
  parameters. Query parameters are usually appended to the URL but in the case
  of HTTP POST they are transferred differently. If you want or need query
  parameters you simply add :query after the url parameter. In the same way, you
  simply add :json after the url parameter if you wrapper needs to support JSON.
  If you need both, simply add both.

  Note that in the case of query or JSON parameters you cannot use wRESTler to
  enforce any structure about your query or JSON data. That is, you cannot
  enforce that your query parameters contain a name like \"answer\" or that
  your JSON object does contain a key like \"advice\". This is just a feature
  that is currently not available for simplicity. Although it would probably be
  not too hard to add it sometime in the future.

  You can now call the function like this:

  (create-new-database \"my-new-database-name\" {\"size\" 42})

  Another example that uses query and JSON parameters:

  (defMyAPI enter-data
  \"Enter data into the database

  (url param) location: location where data should be entered
  (query param) method: How to process the data that should be stored
  (query param) reference: Reference to use for filling in missing data
  (json param) data: A vector of hash-maps each containing the keys \"name\" and \"value\"
  (json param) user: A string containing your user id as the author of your data\"
  :post \"enter_data/$location/\" :query :json)

  And you would call it like this:

  (enter-data
   \"my-folder\"; positional paramater location
   {\"method\" \"make-fancy\" \"reference\" \"old-dataset\"}; query parameters
   {\"data\" [{\"name\" \"Earth\" \"value\" \"mostly harmless\"}]
    \"user\" \"Ford Perfect\"}); JSON parameters

  Note that the JSON parameters are just Clojure values. They will be converted
  to JSON automatically.

  TODO make this customizeable
  When you specify :query and :json for a wrapper the query
  parameters will *always* come first when calling the function.

  Step 4:

  Publish your wrapper on Clojars. I won't explain the details here but I want
  to elaborate on the point that base-url and response-handler are atoms.
  By making them atoms, they can be changed by the user to let the point to a
  different URL or to behave differently when a response is received. So it's
  very important to explain to your users how to access these two atoms so that
  they can configure them for their purposes.

  If you like to see more examples on how to use the library, have a look at
  cyrest-clojure (http://github.com/jbethune/cyrest-clojure).
  "
  (:require [clojure.data.json :as json])
  (:use wrestler.util));TODO update docs

(defmacro defREST-old
  "Declare a new rest API of name api-name with two atoms(!) that contain the
  base-url (common prefix of all REST requests for that API) and a response
  handler function that knows how to work with clj-http responses.
  
  By using this macro a new macro will be created with the name api-name which
  captures the two atoms that you have provided.
  
  For a detailed explanation how to use this macro, read this module's docstring"
  [api-name base-url response-handler]
  `(defmacro ~api-name [~'fun-name ~'fun-doc ~'fun-method ~'fun-url ~'& ~'fun-tags]
        (let [~'tag-set (set ~'fun-tags)
              ~'query-params (when (~'tag-set :query ) ~''query-params)
              ~'data-params (when (~'tag-set :json) ~''data-params)
              ~'url-params (vec (extract-url-params ~'fun-url))
              ~'fun-params (into ~'url-params
                               (for [[~'kw ~'name] {:query ~'query-params :json ~'data-params}
                                     :when (~'tag-set ~'kw)]
                                 ~'name))]
          `(defn ~~'fun-name ~~'fun-doc ~~'fun-params
            ((deref ~~response-handler) (request;maybe (var) is even more helpful here?
                                        ~~'fun-method
                                        (str (deref ~~base-url) (interpolate-url ~~'fun-url ~~'fun-params))
                                        ~~'query-params
                                        ~~'data-params))))))

(defmacro defREST
  "Declare a new rest API of name api-name with two atoms(!) that contain the
  base-url (common prefix of all REST requests for that API) and a response
  handler function that knows how to work with clj-http responses.
  By using this macro a new macro will be created with the name api-name which
  captures the two atoms that you have provided.
  For a detailed explanation how to use this macro, read this module's docstring"
  [api-name base-url response-handler]
  `(defmacro ~api-name [~'fun-name ~'fun-doc ~'fun-method ~'fun-url]
        (let [~'url-params (vec (extract-url-params ~'fun-url))
              ~'fun-params (conj ~'url-params '& ~''special-params)]
          `(defn ~~'fun-name ~~'fun-doc ~~'fun-params
            ((deref ~~response-handler) (request
                                        ~~'fun-method
                                        (str (deref ~~base-url) (interpolate-url ~~'fun-url ~~'url-params))
                                        ~~(quote 'special-params)))))))
