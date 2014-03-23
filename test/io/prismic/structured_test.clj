(ns io.prismic.structured-test
  (:require [clojure.test :refer :all]
            [io.prismic.structured :as structured]
            [io.prismic.api :refer :all]))

(def micro (get-api "https://micro.prismic.io/api"))
(def lbc (get-api "https://lesbonneschoses.prismic.io/api"))
(defn- is= [a b] (is (= a b)))
(defn- resolver [link]
  (let [document (-> link :value :document)]
    (str "http://localhost/" (:type document) "/" (:id document))))

(deftest render-structured

  (testing "render simple structured text"
    (let [doc (get-by-id lbc "UkL0gMuvzYUANCpu")
          expected "<p>Initially started in Paris in 1992, we are now present in <strong>Paris, London, Tokyo and New York</strong>, so you may be lucky with a <em>Les Bonnes Choses</em> shop in your town. We always welcome in our shops the most interested to discover new pastry sensations, and we thrive as we advise you towards your next taste adventures.</p>\n\n<p>If you'd like to challenge us, learn that we like to be challenged! You can place a special order, defining roughly what tastes you like, and how you would like your order to make you feel, and we take it from there!</p>"]
      (is= expected (structured/render (get-fragment doc :content) resolver))))

  (testing "render structured text with groups"
    (let [f {:type "StructuredText"
             :value [{:type "heading2" :text "A tale"}
                     {:type "list-item" :text "firstly"}
                     {:type "list-item" :text "secondly"}
                     {:type "paragraph" :text "finally"}]}
          expected "<h2>A tale</h2>\n\n<ul><li>firstly</li><li>secondly</li></ul>\n\n<p>finally</p>"]
      (is= expected (structured/render f resolver))))

  (let [text "This is a test."
        spans [{:start 5 :end 7 :type "em"}
               {:start 8 :end 9 :type "strong"}]
        block (fn [type] {:type type :text text :spans spans})
        render-block (fn [type] (structured/block (block type) resolver))]

    (testing "render structured text heading"
      (is= "<h1>This is a test.</h1>" (structured/block {:type "heading1" :text text} resolver))
      (is= "<h1>This <em>is</em> <strong>a</strong> test.</h1>" (render-block "heading1"))
      (is= "<h2>This <em>is</em> <strong>a</strong> test.</h2>" (render-block "heading2")))

    (testing "render structured paragraph"
      (is= "<p>This <em>is</em> <strong>a</strong> test.</p>" (render-block "paragraph")))

    (testing "render structured preformatted"
      (is= "<pre>This <em>is</em> <strong>a</strong> test.</pre>" (render-block "preformatted")))

    (testing "render structured unknown span"
      (is= "<span>This <em>is</em> <strong>a</strong> test.</span>" (render-block "")))

    (testing "render structured list item"
      (is= "<li>This <em>is</em> <strong>a</strong> test.</li>" (render-block "o-list-item"))
      (is= "<li>This <em>is</em> <strong>a</strong> test.</li>" (render-block "list-item"))))

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
      (is= expected (structured/block block resolver))))

  (testing "render structured web link"
    (let [spans [{:start 5 :end 7 :type "hyperlink"
                  :data {:type "Link.web"
                         :value {:url "http://prismic.io"}}}]
          block {:type "paragraph" :text "This is a test." :spans spans}
          expected "<p>This <a href=\"http://prismic.io\">is</a> a test.</p>"]
      (is= expected (structured/block block resolver))))

  (testing "render structured file link"
    (let [spans [{:start 5 :end 7 :type "hyperlink"
                  :data {:type "Link.file"
                         :value {:file {:url "http://localhost/doc.pdf"}}}}]
          block {:type "paragraph" :text "This is a test." :spans spans}
          expected "<p>This <a href=\"http://localhost/doc.pdf\">is</a> a test.</p>"]
      (is= expected (structured/block block resolver))))

  (testing "render structured image"
    (let [image {:type "image"
                 :url "http://localhost/img.jpg"
                 :alt "Some alt text"
                 :dimensions {:width 640 :height 666}}
          expected "<p><img src=\"http://localhost/img.jpg\" width=\"640\" height=\"666\" alt=\"Some alt text\" /></p>"]
      (is= expected (structured/block image resolver))))

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
      (is= expected-html (structured/block b resolver)))))
