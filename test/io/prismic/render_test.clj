(ns io.prismic.render-test
  (:require [clojure.test :refer :all]
            [io.prismic.render :as render]
            [io.prismic.api :refer :all]))

(def api (get-api "https://lesbonneschoses.prismic.io/api" nil))
(def api2 (get-api "https://test-public.prismic.io/api" nil))

(defn resolver [link]
  (let [document (-> link :value :document)]
    (str "http://localhost/" (:type document) "/" (:id document))))

(deftest render-fragments
  (testing "render image"
    (let [doc (get-by-bookmark api :stores)
          html (render/fragment (get-fragment doc :image) resolver)
          url "https://prismic-io.s3.amazonaws.com/lesbonneschoses/946cdd210d5f341df7f4d8c7ec3d48adbf7a9d65.jpg"]
      (is (= (str "<img src=\"" url "\" width=\"1500\" height=\"500\" />") html))))
  (testing "render document link"
    (let [doc (get-by-id api "UkL0gMuvzYUANCpi")
          html (render/fragment (get-fragment doc :location) resolver)
          url "http://localhost/store/UkL0gMuvzYUANCpW"]
      (is (= (str "<a href=\"" url "\">new-york-fifth-avenue</a>") html))))
  (testing "render web link"
    (let [doc (get-by-id api2 "Uy4VGQEAAPQzRDR9")
          html (render/web-link (get-fragment doc :related))
          url "https://github.com/prismicio"]
      (is (= (str "<a href=\"" url "\">" url "</a>") html)))))
