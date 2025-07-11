#!/usr/bin/env bb

(require '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.string :as str]
         '[clojure.edn :as edn]
         '[babashka.fs :as fs])

(defn display-usage []
  (println "Usage: bb create-categorization-file-for-current-track.clj [OPTIONS]")
  (println "")
  (println "Categorize the currently playing Spotify track and save results.")
  (println "This script combines track info with categorization data.")
  (println "")
  (println "Options:")
  (println "  --help, -h   Show this help message")
  (println "")
  (println "Workflow:")
  (println "  1. Gets currently playing track info")
  (println "  2. Runs interactive categorization")
  (println "  3. Saves combined data to data/track-cats/")
  (println "")
  (println "Output files:")
  (println "  data/track-cats/<TRACK_ID>_categorization.json")
  (println "")
  (println "Examples:")
  (println "  bb create-categorization-file-for-current-track.clj"))

(defn get-current-track []
  (let [result (process/shell {:out :string :continue true} "bb" "get-current-track.clj" "--edn")]
    (if (= 0 (:exit result))
      (let [track-data (edn/read-string (str/trim (:out result)))]
        (if (:error track-data)
          nil
          track-data))
      (do
        (println "❌ Failed to get current track")
        nil))))

(defn run-categorization []
  (let [result (process/shell {:out :string :continue true} "bb" "get-selections.clj")]
    (if (= 0 (:exit result))
      (do
        (println "✅ Categorization completed")
        true)
      (do
        (println "❌ Failed to run categorization")
        false))))

(defn load-categorization-results []
  (try
    (json/parse-string (slurp "categorization-results.json") true)
    (catch Exception e
      (println "❌ Failed to load categorization results:" (.getMessage e))
      nil)))

(defn ensure-data-directory []
  (let [data-dir "data/track-cats"]
    (when-not (fs/exists? data-dir)
      (fs/create-dirs data-dir)
      (println (str "📁 Created directory: " data-dir)))
    data-dir))

(defn find-next-available-filename [base-filename]
  (if-not (fs/exists? base-filename)
    base-filename
    (loop [counter 1]
      (let [backup-filename (str/replace base-filename "_categorization.json" (str "_categorization_" counter ".json"))]
        (if-not (fs/exists? backup-filename)
          backup-filename
          (recur (inc counter)))))))

(defn save-track-categorization [track categorization data-dir]
  (let [base-filename (str data-dir "/" (:id track) "_categorization.json")
        filename (find-next-available-filename base-filename)
        combined-data {:track-id (:id track)
                       :track-info track
                       :categorization categorization
                       :timestamp (str (java.time.Instant/now))}]
    (when (and (not= filename base-filename) (fs/exists? base-filename))
      (println (str "📁 File already exists, saving as: " (fs/file-name filename))))
    (spit filename (json/generate-string combined-data {:pretty true}))
    filename))

(defn main []
  (when (some #{"--help" "-h"} *command-line-args*)
    (display-usage)
    (System/exit 0))
  
  (println "🎵 Getting current track...")
  (let [track (get-current-track)]
    (if track
      (do
        (println (str "📀 Track: " (:name track) " - " (:artist track)))
        (println (str "📀 Track ID: " (:id track)))
        (println "\n🏷️ Running categorization...")
        (if (run-categorization)
          (let [categorization (load-categorization-results)]
            (if categorization
              (let [data-dir (ensure-data-directory)
                    filename (save-track-categorization track categorization data-dir)]
                (println (str "✅ Saved categorization for track " (:id track) " to " filename)))
              (println "❌ Failed to load categorization results")))
          (println "❌ Categorization failed")))
      (println "❌ No track currently playing or failed to get track ID"))))

(main) 