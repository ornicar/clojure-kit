(ns io.prismic.api-test
  (:require [clojure.test :refer :all]
            [io.prismic.api :refer :all]))

(def api (get-api "https://lesbonneschoses.prismic.io/api" nil))

(deftest create
  (testing "get api"
    (is (= (:oauth_initiate api) "https://lesbonneschoses.prismic.io/auth"))))

(deftest functions
  (testing "get form"
    (is (= (:action (get-form api :everything)) "https://lesbonneschoses.prismic.io/api/documents/search")))
  (testing "get bookmark"
    (is (= (get-bookmark api :jobs) "UkL0gMuvzYUANCpl")))
  (testing "get ref"
    (is (= (get-ref api :Master) {:label "Master" :isMasterRef true :ref "UkL0hcuvzYUANCrm"}))))

(deftest search-documents
  (testing "count blog posts"
    (is (= (:results_size (search api :blog {})) 6)))
  (testing "search"
    (let [id "UkL0gMuvzYUANCpf"
          query {:q (str "[[:d = at(document.id, \"" id "\")]]")}
          response (search api :everything query)]
      (is (= (:id (first (:results response))) id))))
  (testing "by id"
    (let [id "UkL0gMuvzYUANCpf" doc (get-by-id api id)]
      (is (= (:id doc) id))))
  (testing "by bookmark"
    (let [doc (get-by-bookmark api :stores)
          text (:text (first (:value (get-fragment doc :title))))]
      (is (= text "Don't be a stranger!")))))

(deftest select-fragments
  (defn link-slug [link] (get-in link [:value :document :slug]))
  (testing "get image"
    (let [doc (get-by-bookmark api :stores)
          fragment (get-fragment doc :image)]
      (is (= (:type fragment) "Image"))))
  (testing "get one link"
    (let [doc (get-by-id api "UkL0gMuvzYUANCpi")
          link (get-fragment doc :location)]
      (is (= (link-slug link) "new-york-fifth-avenue"))))
  (testing "get all links"
    (let [doc (get-by-id api "UkL0gMuvzYUANCpi")
          links (get-fragments doc :location)]
      (is (= (link-slug (first links)) "new-york-fifth-avenue"))
      (is (= (link-slug (second links)) "tokyo-roppongi-hills")))))
