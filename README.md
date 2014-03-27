## Clojure development kit for prismic.io

### Try it from REPL

```
git clone https://github.com/ornicar/clojure-kit
cd clojure-kit
lein repl

user=> (require '[io.prismic.api :refer :all])

user=> (def prismic (get-api "https://lesbonneschoses.prismic.io/api"))

user=> (def stores (get-by-bookmark prismic :stores))

user=> (:slugs stores)
["dont-be-a-stranger"]

user=> (require '[io.prismic.render :as render])

user=> (render/image (get-fragment stores :image))
"<img src=\"https://prismic-io.s3.amazonaws.com/lesbonneschoses/946cdd210d5f341df7f4d8c7ec3d48adbf7a9d65.jpg\" width=\"1500\" height=\"500\" />"
```

### Continuously run tests while developing

```
lein test-refresh
```
