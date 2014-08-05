(ns io.prismic.test-utils
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]))

(defn pp [arg] (prn arg) arg)

(defn json-mock [file] (json/read-str (slurp (str "resources/mock/" file)) :key-fn keyword))
