(ns io.prismic.api
  (:use [clojure.core.match :only (match)])
  (:require
   [clj-http.client :as http]
   [clojure.data.json :as json]
   [clojure.algo.generic.functor :as gf]))

(defn- authorization-needed [oauth-url]
  (ex-info "You need to provide an access token to access this repository" {:type "AuthorizationNeeded" :oauth-url oauth-url}))

(defn- invalid-token [oauth-url]
  (ex-info "The provided access token is either invalid or expired" {:type "InvalidToken" :oauth-url oauth-url}))

(defn- unexpected-error [msg]
  (ex-info msg {:type "UnexpectedError"}))

(defn get-api
  ([url](get-api url nil))
  ([url token]
     (try
       (let [params (filter #(not (nil? (second %))) {:access_token token :ref ref})]
         (:body (http/get url {:query-params params :accept :json :as :json})))
       (catch clojure.lang.ExceptionInfo e
         (let [data (:object (ex-data e))
               status (:status data)
               body (json/read-str (get-in (ex-data e) [:object :body]))
               oauth-initiate (get body "oauth_initiate")
               error (get body "error")]

           (match [status (nil? oauth-initiate) (nil? token)]
                  [401 false false] (throw (invalid-token oauth-initiate))
                  [401 _ true] (throw (authorization-needed oauth-initiate))
                  [401 _ _] (throw (unexpected-error "Authorization error, but not URL was provided"))
                  :else (throw (unexpected-error (str "Got an HTTP error " status ". " error)))))))))

(defn get-form [api name] (-> api :forms name))
(defn get-bookmark [api name] (-> api :bookmarks name))
(defn get-refs [api] (gf/fmap #(first %) (clojure.walk/keywordize-keys (group-by :label (:refs api)))))
(defn get-ref [api name] (name (get-refs api)))

(defn- vects-to-map [vects] (apply merge (map #(hash-map (keyword (first %1)) (second %1)) vects)))

(defn- search-no-cache
  ([api form-name query](search-no-cache api nil form-name query))
  ([api ref form-name query]
     (let [form (get-form api form-name)
           r (if (nil? ref) (:ref (get-ref api :Master)) ref)
           defaults (vects-to-map (filter (comp not nil? val) (gf/fmap #(:default %) (:fields form))))
           params (merge {:ref r} defaults query)
           response (:body (http/get (:action form) {:query-params params :accept :json :as :json}))
           add-fragments (fn [fragments [k v]]
                           (if (vector? v)
                             (let [make-array (fn [id a] [(keyword (str (name k) "[" id "]")) a])]
                               (merge fragments (vects-to-map (map-indexed make-array v))))
                             (assoc fragments k v)))
           parse-doc (fn [doc] (let [frags1 (get-in doc [:data (keyword (:type doc))])
                                     frags2 (reduce add-fragments {} frags1)]
                                 (assoc (dissoc doc :data) :fragments frags2)))]
       (assoc (dissoc response :results) :results (map parse-doc (:results response))))))

(def search (memoize search-no-cache))

(defn- get-by-id-no-cache
  ([api id] (get-by-id-no-cache api nil id))
  ([api ref id]
  (first (:results (search api ref :everything {:q (str "[[:d = at(document.id, \"" id "\")]]")})))))

(def get-by-id (memoize get-by-id-no-cache))

(defn- get-by-bookmark-no-cache
  ([api bookmark](get-by-bookmark-no-cache api nil bookmark))
  ([api ref bookmark]
     (get-by-id api ref (get-bookmark api bookmark))))

(def get-by-bookmark (memoize get-by-bookmark-no-cache))

(defn get-fragments
  ([doc] (:fragments doc))
  ([doc field] (let [indexed-key #"^([^\[]+)\[\d+\]$"
                     f (fn [[k v]] (when-let [[_ n] (re-matches indexed-key (name k))]
                                     (when (= n (name field)) v)))]
                 (remove nil? (map f (sort (get-fragments doc)))))))

(defn get-fragment [doc field] (or (field (get-fragments doc)) (first (get-fragments doc field))))
