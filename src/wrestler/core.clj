(ns wrestler.core
  "A library for defining REST client wrappers for webservices.

  Defining a wrapper consists of the following steps:
  
  1. Define a default `base-url` and a default `response-handler` as atoms
  2. Create a new wrapper factory with `defREST` and choose a name for it
  3. Define the individual wrappers with the wrapper factory
  4. Ship it to end users who can then `(reset!)` the `base-url` and the
     `response-handler` if necessary.

  In detail this means the following:

  Step 1: Define `base-url` and `response-handler`
  ================================================
  
  Let us assume that every request to the REST API starts with the following
  URL: http://example.com/rest/. Then you should define the following atom:

  ```
  (def base-url (atom \"http://example.com/rest/\"))
  ```

  And you need to define a function that will handle responses from the server
  and wrap this function with an atom:

  ```
  (def response-handler (atom
                          (fn [response]
                             (print \"Got an answer from the server!\")
                             (:body response))))
  ```

  The `response` parameter is a hash-map returned by the clj-http library
  (https://github.com/dakrone/clj-http/)
  

  Alternatively you can just use one of the handlers from
  [`wrestler.handlers`](wrestler.handlers.html) but don't forget to call (atom)
  on them.
  
  Step 2: Create a new wrapper factory
  ====================================
  
  ```
  (defREST
    defMyAPI; This is the name of your new API wrapper
   'current-namespace/base-url
   'current-namespace/response-handler)
  ```

  Note the fully-qualified and quoted form of the `base-url` and
  `response-handler` atoms. These are required for the macros to work properly
  when they are used in the context of a different namespace (If you know a
  cleaner solution, I'd love to hear it!)

  Next, we will see how to use it.

  Step 3: Define the individual wrappers
  ======================================
  
  Create the individual wrappers

  ```
  (defMyAPI get-database-size
  \"This is a mandatory docstring that describes the new function get-database-size\"
  :get \"subpath/that/is/appended/to/base-url/\")
  ```

  The newly created wrapper factory `defMyAPI` has the following parameters:

     1. Name of the wrapper function that will be created: `get-database-size`
     2. Docstring of the wrapper function that will be created: `\"This is a mandatory...\"`
     3. HTTP method. Pick one of `:get :post :put :del`. Here it's `:get`
     4. URL extension to the base URL. The base url and this URL will be glued
        together. This extension URL can also contain positional parameters
        defined with a leading $ (more on that later):
        `\"subpath/that/is/appended/...\"`

  You would call this function just like this:

  ```
  (get-database-size)
  ```

  Another example:

  ```
  (defMyAPI create-new-database
  \"Write documentation here
  
  (url param) name: Name of the new database
  (query param) size: Initial size of the database

  Some more documentation text.\"
  :post \"create_database/$name\")
  ```

  All normal positional parameters are defined with a $ followed by the name of
  the parameter. In this example the only parameter is called \"name\" and is
  written as `$name`. Parameter names expand up to the nearest slash or dot
  (`/` or `.`). You can have as many url parameters as you like:
  `literal/$var1/$var2/literal/$var3.zip`

  Further parameters that are being passed to the function are treated as
  key-value pairs and are turned into query parameters unless one of the keys is
  a special key like `:json`.

  So if you call the function like this:

  ```
  (create-new-database \"all-my-data\" :initial-size 42 :cache 1)
  ```

  The URL `@base-url` + `create_database/all-my-data?initial-size=42&cache=1`
  will be requested. While it is usually more clojurerish to use keywords as
  keys, you can also use strings as keys if that is more convenient for you.

  Sometimes you need to send some payload in addition to your request. The most
  popular way to do this nowadays is to use JSON. In order to send JSON data to
  the server, simply pass a `:json` key and some clojure value as additional
  arguments to a function call. Example:

  ```
  (defMyAPI enter-data
  \"Enter data into the database

  (url param) location: location where data should be entered
  (query param) method: How to process the data that should be stored
  (query param) reference: Reference to use for filling in missing data
  (json param) data: A vector of hash-maps each containing the keys \"name\" and \"value\"
  (json param) user: A string containing your user id as the author of your data\" ; end of docstring
  :post \"enter_data/$location/\")
  ```

  And you would call it like this:

  ```
  (enter-data
   \"my-folder\"; positional parameter location
   :method \"make fancy\" :reference \"old dataset\";regular query paramters
   :json {\"data\" [{\"name\" \"Earth\" \"value\" \"mostly harmless\"}]
          \"user\" \"Ford Perfect\"}); JSON parameters
  ```

  Note that the JSON parameters are just Clojure values. They will be converted
  to JSON automatically.

  And also note that we did not specify explicitly that the function can take
  JSON values. All functions created with wRESTler support sending additional
  query values and JSON values by default. This design decision was made because
  some REST APIs have optional query and/or JSON parameters. To make wRESTler
  REPL-friendly I decided to use varargs for query and JSON parameters at the
  same time. This leads to slightly less arity checking but should make your
  workflow much nicer and your code more readable.

  Thus, my advice is to just write all mandatory and optional parameters in the
  docstring so that the user can see how to use a wrapper function. If the user
  doesn't use the wrapper correctly the server will probably return an error
  message. In this case the docstring should explain how to use the wrapper
  properly.


  Step 4: Ship it
  ===============

  Publish your wrapper on Clojars. I won't explain the details here but I want
  to emphasize the importance of your `base-url` and your `response-handler`
  being atoms. By making them atoms, they can be changed by the user to let
  them point to a different URL or to behave differently when a response is
  received.  So it's very important to explain to your users how to access these
  two atoms so that they can configure them for their purposes.

  If you like to see more examples on how to use the library, have a look at
  cyrest-clojure (http://github.com/jbethune/cyrest-clojure).
  "
  {:doc/format :markdown}
  (:use wrestler.util))


(defmacro defREST
  "Declare a new rest API of name api-name with two atoms(!) that contain the
  base-url (common URL prefix of all REST requests for that API) and a response
  handler function that knows how to work with clj-http responses.
  By using this macro a new macro will be created with the name api-name which
  captures the two atoms that you have provided.
  For a detailed explanation how to use this macro, read this docstring of the
  wrestler.core namespace."
  [api-name base-url response-handler]
  `(defmacro ~api-name [~'fun-name ~'fun-doc ~'fun-method ~'fun-url]
        (let [~'url-params (vec (extract-url-params ~'fun-url))
              ~'fun-params (conj ~'url-params '& ~''special-params)]
          `(defn ~~'fun-name ~~'fun-doc ~~'fun-params
            ((deref ~~response-handler) (request
                                        ~~'fun-method
                                        (str (deref ~~base-url) (interpolate-url ~~'fun-url ~~'url-params))
                                        ~~(quote 'special-params)))))))
