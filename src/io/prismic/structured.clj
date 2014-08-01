(ns io.prismic.structured
  (:require [clojure.string :as str])
  (:use [clojure.core.match :only (match)]))

(defn- in-tag [t content] (str "<" t ">" content "</" t ">"))

(defn- escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String (as-str text)
      (replace "&"  "&amp;")
      (replace "<"  "&lt;")
      (replace ">"  "&gt;")
      (replace "\"" "&quot;")))

(defn text [text spans resolver]
  (letfn [(write-link [f]
            (case (:type f)
              "Link.document" (str "<a href=\"" (resolver f) "\">")
              "Link.file" (str "<a href=\"" (-> f :value :file :url) "\">")
              "Link.web" (str "<a href=\"" (-> f :value :url) "\">")
              ""))
          (write-tag [span opening]
            (case (:type span)
              "em" (if opening "<em>" "</em>")
              "strong" (if opening "<strong>" "</strong>")
              "hyperlink" (if opening (write-link (:data span)) "</a>")
              (if opening "<span>" "</span>")))
          (write-html [endings startings]
            (str/join (concat (map #(write-tag % false) endings)
                              (map #(write-tag % true) startings))))]
    (loop [in (vec (map-indexed vector text))
           startings (sort-by :start (filter #(< (:start %1) (:end %1)) spans))
           endings []
           html []]
      (let [nexts (remove nil? [(-> startings first :start) (-> endings first :end)])
            next-op (if (empty? nexts) nil (apply min nexts))]
        (match in [[pos current] & tail]
               (if (= next-op pos)
                 (let [[endings-to-apply other-endings] (split-with #(= (:end %) pos) endings)
                       [startings-to-apply other-startings] (split-with #(= (:start %) pos) startings)
                       new-html (str (write-html endings-to-apply startings-to-apply) (escape-html current))
                       new-endings (concat (reverse startings-to-apply) other-endings)]
                   (recur tail other-startings new-endings (conj html new-html)))
                 (let [[done todo] (split-with #(not (= (first %) next-op)) in)
                       new-html (escape-html (str/join (map second done)))]
                   (recur (vec todo) startings endings (conj html new-html))))
               :else (str/join (conj html (write-html endings []))))))))

(defn block [b resolver]
  (letfn [(text-html [] (text (:text b) (:spans b) resolver))
          (image-html [] (str "<img src=\"" (:url b) "\" "
                              "width=\"" (-> b :dimensions :width) "\" "
                              "height=\"" (-> b :dimensions :height) "\" "
                              "alt=\"" (:alt b) "\" />"))
          (embed-html [e] (str "<div data-oembed=\"" (:embed_url e) "\" "
                               "data-oembed-type=\"" (str/lower-case (:type e)) "\" "
                               "data-oembed-provider=\"" (str/lower-case (:provider_name e)) "\">"
                               (:html e) "</div>"))
          (heading [level content] (in-tag (str "h" level) content))]
    (case (:type b)
      "heading1" (heading 1 (text-html))
      "heading2" (heading 2 (text-html))
      "heading3" (heading 3 (text-html))
      "heading4" (heading 4 (text-html))
      "paragraph" (in-tag "p" (text-html))
      "preformatted" (in-tag "pre" (text-html))
      "list-item" (in-tag "li" (text-html))
      "o-list-item" (in-tag "li" (text-html))
      "embed" (embed-html (:oembed b))
      "image" (in-tag "p" (image-html))
      (in-tag "span" (text-html)))))

(defn render [f resolver]
  "Serializes the current StructuredText fragment into a fully usable HTML code.
   You need to pass a proper link_resolver so that internal links are turned into the proper URL in
   your website. If you use a starter kit, one is provided, that you can still update later."
  (let [reducer (fn [[g & gs :as all] b]
                  (match [(:tag g) (:type b)]
                         ["ul" "list-item"] (into [(assoc g :blocks (conj (:blocks g) b))] gs)
                         ["ol" "o-list-item"] (into [(assoc g :blocks (conj (:blocks g) b))] gs)
                         [_ "list-item"] (into [{:tag "ul" :blocks [b]}] all)
                         [_ "o-list-item"] (into [{:tag "ol" :blocks [b]}] all)
                         :else (into [{:blocks [b]}] all)))
        groups (reverse (reduce reducer [] (:value f)))
        render-group (fn [g] (let [html (str/join (map #(block % resolver) (:blocks g)))]
                               (if (nil? (:tag g)) html (in-tag (:tag g) html))))]
    (str/join "\n\n" (map render-group groups))))
