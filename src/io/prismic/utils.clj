(ns io.prismic.utils)

; private utilities

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String (str text)
      (replace "&"  "&amp;")
      (replace "<"  "&lt;")
      (replace ">"  "&gt;")
      (replace "\"" "&quot;")))
