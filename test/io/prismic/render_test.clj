(ns io.prismic.render-test
  (:require [clojure.test :refer :all]
            [io.prismic.render :as render]
            [io.prismic.structured :as structured]
            [io.prismic.api :refer :all]))

(def lbc (get-api "https://lesbonneschoses.prismic.io/api"))
(def public (get-api "https://test-public.prismic.io/api"))
(defn- is= [a b] (is (= a b)))
(defn- resolver [link]
  (let [document (-> link :value :document)]
    (str "http://localhost/" (:type document) "/" (:id document))))

(deftest render-fragments

  (testing "render image"
    (let [doc (get-by-bookmark lbc :stores)
          html (render/image (get-fragment doc :image))
          url "https://prismic-io.s3.amazonaws.com/lesbonneschoses/946cdd210d5f341df7f4d8c7ec3d48adbf7a9d65.jpg"]
      (is= (str "<img src=\"" url "\" width=\"1500\" height=\"500\" />") html)))

  (testing "render document link"
    (let [doc (get-by-id lbc "UkL0gMuvzYUANCpi")
          html (render/document-link (get-fragment doc :location) resolver)
          url "http://localhost/store/UkL0gMuvzYUANCpW"]
      (is= (str "<a href=\"" url "\">new-york-fifth-avenue</a>") html)))

  (testing "render web link"
    (let [doc (get-by-id public "Uy4VGQEAAPQzRDR9")
          html (render/web-link (get-fragment doc :related))
          url "https://github.com/prismicio"]
      (is= (str "<a href=\"" url "\">" url "</a>") html)))

  (testing "render file link"
    (let [doc (get-by-id public "Uy4VGQEAAPQzRDR9")
          html (render/file-link (get-fragment doc :download))
          url "https://prismic-io.s3.amazonaws.com/test-public%2Feb14f588-07b4-4df7-be43-5b6f6383d202_ambiance-radio.m3u"]
      (is= (str "<a href=\"" url "\">ambiance-radio.m3u</a>") html)))

  (testing "render number"
    (let [doc (get-by-id lbc "UkL0gMuvzYUANCpT")
          html (render/number (get-fragment doc :price))]
      (is= "<span class=\"number\">3.0</span>" html)))

  (testing "render color"
    (let [doc (get-by-id lbc "UkL0gMuvzYUANCpT")
          html (render/color (get-fragment doc :color))]
      (is= "<span class=\"color\">#f9001b</span>" html)))

  (testing "render date"
    (let [doc (get-by-id lbc "UkL0gMuvzYUANCpn")
          html (render/date (get-fragment doc :date))]
      (is= "<span class=\"date\">2013/08/17</span>" html)))

  (testing "render date with custom pattern"
    (let [doc (get-by-id lbc "UkL0gMuvzYUANCpn")
          html (render/date (get-fragment doc :date) "dd.MM.yyy")]
      (is= "<span class=\"date\">17.08.2013</span>" html)))

  (testing "render text"
    (let [doc (get-by-id lbc "UkL0gMuvzYUANCpn")
          html (render/text (get-fragment doc :author))]
      (is= "<span class=\"text\">Tsutomu Kabayashi, Pastry Dresser</span>" html))))
