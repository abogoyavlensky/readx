(ns readx.utils.bionic
  (:import (java.util.regex Pattern)))

(def ^:private ^Pattern word-pattern
  (Pattern/compile "\\p{L}+|[^\\p{L}]+"))

(defn to-bionic-text
  "Convert `text` to bionic reading format as HTML string.
  Bolds the first ceil(len/2) characters of each word with `<b>` tags."
  [text]
  (let [matcher (.matcher word-pattern text)
        sb (StringBuilder.)]
    (while (.find matcher)
      (let [token (.group matcher)]
        (if (Character/isLetter (.charAt token 0))
          (let [bold-len (int (Math/ceil (/ (count token) 2.0)))]
            (.append sb "<b>")
            (.append sb (subs token 0 bold-len))
            (.append sb "</b>")
            (.append sb (subs token bold-len)))
          (.append sb token))))
    (.toString sb)))
