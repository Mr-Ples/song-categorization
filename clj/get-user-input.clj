#!/usr/bin/env bb

(require '[babashka.process :as process]
         '[clojure.string :as str])

(defn display-usage []
  (println "Usage: bb get-user-input.clj <HEADER> <ITEM1> [ITEM2] [ITEM3] ...")
  (println "")
  (println "Interactive fuzzy selection tool using fzf.")
  (println "Allows selecting multiple items and adding new ones.")
  (println "")
  (println "Arguments:")
  (println "  HEADER    The prompt text to display")
  (println "  ITEM*     List of selectable items")
  (println "")
  (println "Controls:")
  (println "  Tab       Select an item")
  (println "  Comma     Separate multiple new items when typing")
  (println "  Enter     Confirm selection")
  (println "")
  (println "Examples:")
  (println "  bb get-user-input.clj \"Choose genre\" rock pop jazz")
  (println "  bb get-user-input.clj \"Select mood\" happy sad energetic"))

(defn fuzzy-create [items header]
  (let [items-str (str/join "\n" items)
        prompt (str header " (use Tab to select, comma to separate new items): ")
        result (process/shell {:out :string :in items-str :continue true} 
                             "fzf" "--print-query" "--exact" "--multi" "--prompt" prompt)
        lines (str/split-lines (:out result))
        query (first lines)
        selections (if (> (count lines) 1) (rest lines) [])]
    ;; Return selections if they exist, otherwise parse query for multiple items
    (if (not (empty? selections))
      selections
      ;; Split query by comma and trim whitespace to allow multiple new items
      ;; Handle case where query might be nil or empty
      (if (and query (not (str/blank? query)))
        (map str/trim (str/split query #","))
        []))))

(defn main [& args]
  (when (some #{"--help" "-h"} args)
    (display-usage)
    (System/exit 0))
  
  (when (< (count args) 2)
    (display-usage)
    (System/exit 1))
  
  (let [header (first args)
        items (rest args)
        chosen (fuzzy-create items header)]
    ;; Print each selection on a separate line
    (doseq [choice chosen]
      (when (not (str/blank? choice))
        (println choice)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*)) 