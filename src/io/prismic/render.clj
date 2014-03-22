(ns io.prismic.render)

(defn image [f]
  (let [main (-> f :value :main) size (:dimensions main)]
    (str "<img src=\"" (:url main) "\" width=\"" (:width size) "\" height=\"" (:height size) "\" />")))

(defn web-link [f] (str "<a href=\"" (-> f :value :url) "\">" (-> f :value :url) "</a>"))

(defn document-link [f resolver]
  (str "<a href=\"" (resolver f) "\">" (-> f :value :document :slug) "</a>"))

(defn structured-text [f] (-> f :value :text))

(defn fragment [f resolver]
  (case (:type f)
    "StructuredText" (structured-text f)
    "Image" (image f)
    "Link.document" (document-link f resolver)
    "Link.web" (web-link f resolver)
    ""))
