#!/usr/bin/env bb

(require '[cheshire.core :as json]
         '[babashka.process :as process]
         '[clojure.string :as str])

(defn display-usage []
  (println "Usage: bb get-selections.clj [OPTIONS]")
  (println "")
  (println "Interactive categorization tool that prompts for selections")
  (println "based on the category structure defined in category-structure.json.")
  (println "")
  (println "Options:")
  (println "  --help, -h   Show this help message")
  (println "")
  (println "Files:")
  (println "  Input:  category-structure.json")
  (println "  Output: categorization-results.json")
  (println "          category-structure.json (updated with new options)")
  (println "")
  (println "The script will prompt you to categorize based on predefined")
  (println "categories. You can select existing options or add new ones.")
  (println "")
  (println "Examples:")
  (println "  bb get-selections.clj"))

(defn collect-selections []
  (let [categories (json/parse-string (slurp "category-structure.json") true)
        selections {}
        updated-categories (atom categories)]
    [(reduce (fn [acc category]
               (if (:trigger category)
                 acc ; Skip trigger categories for now
                 (let [label (:label category)
                       options (:options category)
                       extra-options (get category :extra_options [])
                       ;; Create combined options with null option first, then separator if both exist
                       combined-options (cond
                                          (and (seq options) (seq extra-options))
                                          (concat ["(none)"] options ["--- Extra Options ---"] extra-options)
                                          
                                          (seq options)
                                          (concat ["(none)"] options)
                                          
                                          (seq extra-options)
                                          (concat ["(none)"] ["--- Extra Options ---"] extra-options)
                                          
                                          :else
                                          ["(none)"])
                       args (into [label] combined-options)
                       result (apply process/shell {:out :string :continue true} "bb" "get-user-input.clj" args)
                       user-selections (str/split-lines (str/trim (:out result)))
                       ;; Convert separator selections to "(none)" and filter out blank selections
                       filtered-selections (filter #(not (str/blank? %))
                                                   (map #(if (str/starts-with? % "--- Extra Options ---")
                                                           "(none)"
                                                           %)
                                                        user-selections))]
                   (if (empty? filtered-selections)
                     acc
                     (let [all-real-options (concat ["(none)"] options extra-options)
                           new-options (filter #(not (contains? (set all-real-options) %)) filtered-selections)]
                       ;; Update category structure if there are new options
                       (when (seq new-options)
                         (swap! updated-categories
                                (fn [cats]
                                  (mapv (fn [cat]
                                          (if (= (:label cat) label)
                                            (assoc cat :extra_options 
                                                   (vec (distinct (concat (get cat :extra_options []) new-options))))
                                            cat))
                                        cats))))
                       (assoc acc (keyword label) filtered-selections))))))
             selections
             categories)
     @updated-categories]))

(defn save-selections-to-file [selections filename]
  (spit filename (json/generate-string selections {:pretty true})))

(defn save-category-structure [categories filename]
  (spit filename (json/generate-string categories {:pretty true})))

(defn main []
  (when (some #{"--help" "-h"} *command-line-args*)
    (display-usage)
    (System/exit 0))
  
  (let [[selections updated-categories] (collect-selections)
        filename "categorization-results.json"
        category-file "category-structure.json"]
    (if (empty? selections)
      (println "No selections made.")
      (do
        (save-selections-to-file selections filename)
        (save-category-structure updated-categories category-file)
        (println (str "Selections saved to " filename))
        (println "Results:")
        (doseq [[category values] selections]
          (println (str "  " (name category) ": " (str/join ", " values))))))))

(main)