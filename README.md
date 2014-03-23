## Clojure development kit for prismic.io

### Try it from REPL

```
git clone https://github.com/ornicar/clojure-kit
cd clojure kit
lein repl

io.prismic.api=> (def prismic (get-api "https://lesbonneschoses.prismic.io/api"))
#'io.prismic.api/prismic
io.prismic.api=> (def stores (get-by-bookmark prismic :stores))
#'io.prismic.api/stores
io.prismic.api=> (:slugs stores)
["dont-be-a-stranger"]
io.prismic.api=> (require '[io.prismic.render :as render])
nil
io.prismic.api=> (render/image (get-fragment stores :image))
"<img src=\"https://prismic-io.s3.amazonaws.com/lesbonneschoses/946cdd210d5f341df7f4d8c7ec3d48adbf7a9d65.jpg\" width=\"1500\" height=\"500\" />"
io.prismic.api=>
```
