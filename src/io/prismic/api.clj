(ns io.prismic.api
  (:require [clj-http.client :as http]
            [clojure.algo.generic.functor :as gf]))

(defn get-api
  ([url] (get-api url nil))
  ([url token] (:body (http/get url {:query-params (if (nil? token) {} {:token token})
                                     :accept :json :as :json}))))

(defn get-form [api name] (get-in api [:forms name]))
(defn get-bookmark [api name] (get-in api [:bookmarks name]))
(defn get-refs [api] (gf/fmap #(first %) (clojure.walk/keywordize-keys (group-by :label (:refs api)))))
(defn get-ref [api name] (name (get-refs api)))

(defn- vects-to-map [vects] (apply merge (map #(hash-map (keyword (first %1)) (second %1)) vects)))

(defn- search-no-cache [api form-name query]
  (let [form (get-form api form-name)
        defaults (vects-to-map (filter (comp not nil? val) (gf/fmap #(:default %) (:fields form))))
        params (merge {:ref (:ref (get-ref api :Master))} defaults query)
        response (:body (http/get (:action form) {:query-params params :accept :json :as :json}))
        add-fragments (fn [fragments [k v]]
                        (if (vector? v)
                          (let [make-array (fn [id a] [(keyword (str (name k) "[" id "]")) a])]
                            (merge fragments (vects-to-map (map-indexed make-array v))))
                          (assoc fragments k v)))
        parse-doc (fn [doc] (let [frags1 (get-in doc [:data (keyword (:type doc))])
                                  frags2 (reduce add-fragments {} frags1)]
                              (assoc (dissoc doc :data) :fragments frags2)))]
    (assoc (dissoc response :results) :results (map parse-doc (:results response)))))

(def search (memoize search-no-cache))

(defn- get-by-id-no-cache [api id]
  (first (:results (search api :everything {:q (str "[[:d = at(document.id, \"" id "\")]]")}))))

(def get-by-id (memoize get-by-id-no-cache))

(defn- get-by-bookmark-no-cache [api bookmark] (get-by-id api (get-bookmark api bookmark)))

(def get-by-bookmark (memoize get-by-bookmark-no-cache))

(defn get-fragments
  ([doc] (:fragments doc))
  ([doc field] (let [indexed-key #"^([^\[]+)\[\d+\]$"
                     f (fn [[k v]]
                         (when-let [[_ n] (re-matches indexed-key (name k))]
                           (when (= n (name field)) v)))]
                 (remove nil? (map f (sort (get-fragments doc)))))))

(defn get-fragment [doc field] (or (field (get-fragments doc)) (first (get-fragments doc field))))
