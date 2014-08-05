(ns io.prismic.structured-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [io.prismic.test-utils :refer :all]
            [io.prismic.structured :as structured]
            [io.prismic.api :refer :all]))

(def micro (get-api "https://micro.prismic.io/api"))
(def lbc (get-api "https://lesbonneschoses.prismic.io/api"))
(defn- resolver [link]
  (let [document (-> link :value :document)]
    (str "http://localhost/" (:type document) "/" (:id document))))

(deftest render-structured

  (testing "render simple structured text"
    (let [fragment (json-mock "structured_text_linkfile.json")
          expected "<p><a href=\"https://prismic-io.s3.amazonaws.com/annual.report.pdf\">2012 Annual Report</a></p>\n\n<p><a href=\"https://prismic-io.s3.amazonaws.com/annual.budget.pdf\">2012 Annual Budget</a></p>\n\n<p><a href=\"https://prismic-io.s3.amazonaws.com/vision.strategic.plan_.sm_.pdf\">2015 Vision &amp; Strategic Plan</a></p>"]
      (is (= expected (structured/render fragment resolver)))))

  (testing "render structured text with groups"
    (let [f {:type "StructuredText"
             :value [{:type "heading2" :text "A tale"}
                     {:type "list-item" :text "firstly"}
                     {:type "list-item" :text "secondly"}
                     {:type "paragraph" :text "finally"}]}
          expected "<h2>A tale</h2>\n\n<ul><li>firstly</li><li>secondly</li></ul>\n\n<p>finally</p>"]
      (is (= expected (structured/render f resolver)))))

  (let [text "This is a test."
        spans [{:start 5 :end 7 :type "em"}
               {:start 8 :end 9 :type "strong"}]
        block (fn [type] {:type type :text text :spans spans})
        render-block (fn [type] (structured/block (block type) resolver))]

    (testing "render structured text heading"
      (is (= "<h1>This is a test.</h1>" (structured/block {:type "heading1" :text text} resolver)))
      (is (= "<h1>This <em>is</em> <strong>a</strong> test.</h1>" (render-block "heading1")))
      (is (= "<h2>This <em>is</em> <strong>a</strong> test.</h2>" (render-block "heading2"))))

    (testing "render structured paragraph"
      (is (= "<p>This <em>is</em> <strong>a</strong> test.</p>" (render-block "paragraph"))))

    (testing "render structured preformatted"
      (is (= "<pre>This <em>is</em> <strong>a</strong> test.</pre>" (render-block "preformatted"))))

    (testing "render structured unknown span"
      (is (= "<span>This <em>is</em> <strong>a</strong> test.</span>" (render-block ""))))

    (testing "render structured list item"
      (is (= "<li>This <em>is</em> <strong>a</strong> test.</li>" (render-block "o-list-item")))
      (is (= "<li>This <em>is</em> <strong>a</strong> test.</li>" (render-block "list-item")))))

  (testing "render structured document link"
    (let [spans [{:start 5 :end 7 :type "hyperlink"
                  :data {:type "Link.document"
                         :value {:document {:id "UkL0gMuvzYUANCpf"
                                            :type "job-offer"
                                            :tags [ ]
                                            :slug "pastry-dresser"}
                                 :isBroken false}}}]
          block {:type "paragraph" :text "This is a test." :spans spans}
          expected "<p>This <a href=\"http://localhost/job-offer/UkL0gMuvzYUANCpf\">is</a> a test.</p>"]
      (is (= expected (structured/block block resolver)))))

  (testing "render structured web link"
    (let [spans [{:start 5 :end 7 :type "hyperlink"
                  :data {:type "Link.web"
                         :value {:url "http://prismic.io"}}}]
          block {:type "paragraph" :text "This is a test." :spans spans}
          expected "<p>This <a href=\"http://prismic.io\">is</a> a test.</p>"]
      (is (= expected (structured/block block resolver)))))

  (testing "render structured file link"
    (let [spans [{:start 5 :end 7 :type "hyperlink"
                  :data {:type "Link.file"
                         :value {:file {:url "http://localhost/doc.pdf"}}}}]
          block {:type "paragraph" :text "This is a test." :spans spans}
          expected "<p>This <a href=\"http://localhost/doc.pdf\">is</a> a test.</p>"]
      (is (= expected (structured/block block resolver)))))

  (testing "render structured image"
    (let [image {:type "image"
                 :url "http://localhost/img.jpg"
                 :alt "Some alt text"
                 :dimensions {:width 640 :height 666}}
          expected "<p><img src=\"http://localhost/img.jpg\" width=\"640\" height=\"666\" alt=\"Some alt text\" /></p>"]
      (is (= expected (structured/block image resolver)))))

  (testing "render structured embed"
    (let [b {:type "embed"
             :oembed {:thumbnail_url "http://i1.ytimg.com/vi/Ye78F3-CuXY/hqdefault.jpg"
                      :provider_url "http://www.youtube.com/"
                      :thumbnail_height 360
                      :type "video"
                      :version "1.0"
                      :author_url "http://www.youtube.com/user/thatsmynamedude"
                      :width 459
                      :author_name "TRR56"
                      :title "How It's Made Chocolate"
                      :thumbnail_width 480
                      :height 344
                      :provider_name "YouTube"
                      :html "<iframe width=\"459\" height=\"344\" src=\"http://www.youtube.com/embed/Ye78F3-CuXY?feature=oembed\" frameborder=\"0\" allowfullscreen></iframe>"
                      :embed_url "http://www.youtube.com/watch?v=Ye78F3-CuXY"}}
          expected-html "<div data-oembed=\"http://www.youtube.com/watch?v=Ye78F3-CuXY\" data-oembed-type=\"video\" data-oembed-provider=\"youtube\"><iframe width=\"459\" height=\"344\" src=\"http://www.youtube.com/embed/Ye78F3-CuXY?feature=oembed\" frameborder=\"0\" allowfullscreen></iframe></div>"]
      (is (= expected-html (structured/block b resolver))))))
