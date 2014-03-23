(ns io.prismic.api-test
  (:require [clojure.test :refer :all]
            [io.prismic.api :refer :all]))

(def lbc (get-api "https://lesbonneschoses.prismic.io/api"))
(def micro (get-api "https://micro.prismic.io/api"))
(defn- is= [a b] (is (= a b)))

(deftest create
  (testing "get api"
    (is= (:oauth_initiate lbc) "https://lesbonneschoses.prismic.io/auth")))

(deftest api-functions

  (testing "get form"
    (is= (:action (get-form lbc :everything)) "https://lesbonneschoses.prismic.io/api/documents/search"))

  (testing "get bookmark"
    (is= (get-bookmark lbc :jobs) "UkL0gMuvzYUANCpl"))

  (testing "get ref"
    (is= (get-ref lbc :Master) {:label "Master" :isMasterRef true :ref "UkL0hcuvzYUANCrm"})))

(deftest search-documents

  (testing "count blog posts"
    (is= (:results_size (search lbc :blog {})) 6))

  (testing "fulltext search"
    (let [query {:q (str "[[:d = fulltext(my.job-offer.name, \"Pastry dresser\")]]")}
          response (search lbc :everything query)]
      (is= (-> (:results response) first :id) "UkL0gMuvzYUANCpf")))

  (testing "find by id"
    (let [id "UkL0gMuvzYUANCpf" doc (get-by-id lbc id)]
      (is= (:id doc) id)))

  (testing "find by bookmark"
    (let [doc (get-by-bookmark lbc :stores)
          text (-> (get-fragment doc :title) :value first :text)]
      (is= text "Don't be a stranger!"))))


(deftest select-fragments
  (let [link-slug (fn [link] (-> link :value :document :slug))
        job (get-by-id lbc "UkL0gMuvzYUANCpi")
        post (get-by-id micro "UrDcEAEAAKUbpbND")]

    (testing "get image"
      (is= (:type (get-fragment job :name)) "StructuredText"))

    (testing "get one link"
      (is= (link-slug (get-fragment job :location)) "new-york-fifth-avenue"))

    (testing "get all links"
      (let [links (get-fragments job :location)]
        (is= (link-slug (first links)) "new-york-fifth-avenue")
        (is= (link-slug (second links)) "tokyo-roppongi-hills")))

    (testing "get structured text"
      (let [f (get-fragment post :docs)]
        (is= "UrDejAEAAFwMyrW9" (-> f :value first :linktodoc :value :document :id))
        (is= "paragraph" (-> f :value first :desc :value first :type))
        (is= "UrDmKgEAALwMyrXA" (-> f :value second :linktodoc :value :document :id))))))
