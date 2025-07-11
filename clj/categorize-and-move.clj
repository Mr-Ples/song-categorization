#!/usr/bin/env bb

(require '[babashka.process :as process]
         '[clojure.string :as str])

(defn display-usage []
  (println "Usage: bb categorize-and-move.clj <TARGET_PLAYLIST_ID>")
  (println "       bb categorize-and-move.clj <SOURCE_PLAYLIST_ID> <TARGET_PLAYLIST_ID>")
  (println "")
  (println "This script will:")
  (println "1. Run categorization on the current track")
  (println "2. If successful, add track to target playlist (1 arg) or move from source to target (2 args)")
  (println "")
  (println "Examples:")
  (println "  # Add to target playlist only")
  (println "  bb categorize-and-move.clj 6wsBrS9qpdSA5kSnwdJM3g")
  (println "  # Move between playlists")
  (println "  bb categorize-and-move.clj 1it5Tbzz9tJ4464wp6oq5N 6wsBrS9qpdSA5kSnwdJM3g"))

(defn run-categorization []
  (println "🏷️ Running track categorization...")
  (let [result (process/shell {:out :string :err :string :continue true} 
                             "bb" "create-categorization-file-for-current-track.clj")]
    (if (= 0 (:exit result))
      (do
        (println "✅ Categorization completed successfully")
        true)
      (do
        (println "❌ Categorization failed:")
        (when-not (str/blank? (:err result))
          (println (:err result)))
        false))))

(defn get-current-track-id []
  (let [result (process/shell {:out :string :continue true} "bb" "get-current-track.clj" "--edn")]
    (if (= 0 (:exit result))
      (let [track-data (read-string (str/trim (:out result)))]
        (if (:error track-data)
          nil
          (:id track-data)))
      nil)))

(defn move-track 
  ([track-id target-playlist-id]
   (println "📦 Adding track to playlist...")
   (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "move-track.clj" track-id target-playlist-id)]
     (if (= 0 (:exit result))
       (do
         (println "✅ Track added successfully")
         true)
       (do
         (println "❌ Failed to add track:")
         (when-not (str/blank? (:err result))
           (println (:err result)))
         false))))
  ([track-id source-playlist-id target-playlist-id]
   (println "📦 Moving track between playlists...")
   (let [result (process/shell {:out :string :err :string :continue true}
                              "bb" "move-track.clj" track-id source-playlist-id target-playlist-id)]
     (if (= 0 (:exit result))
       (do
         (println "✅ Track moved successfully")
         true)
       (do
         (println "❌ Failed to move track:")
         (when-not (str/blank? (:err result))
           (println (:err result)))
         false)))))

(defn main [& args]
  (when (some #{"--help" "-h"} args)
    (display-usage)
    (System/exit 0))
  
  (when (not (or (= 1 (count args)) (= 2 (count args))))
    (display-usage)
    (System/exit 1))
  
  (let [is-move-operation (= 2 (count args))
        target-playlist-id (if is-move-operation (second args) (first args))
        source-playlist-id (when is-move-operation (first args))]
    
    (println "🎵 Categorize and Move Workflow")
    (println "===============================")
    (when source-playlist-id
      (println (str "Source Playlist: " source-playlist-id)))
    (println (str "Target Playlist: " target-playlist-id))
    (println "")
    
    ;; Get current track ID first
    (let [track-id (get-current-track-id)]
      (if track-id
        (do
          (println (str "🎶 Current Track ID: " track-id))
          (println "")
          
          ;; Step 1: Run categorization
          (if (run-categorization)
            ;; Step 2: Move/add track if categorization succeeded
            (if (if is-move-operation
                  (move-track track-id source-playlist-id target-playlist-id)
                  (move-track track-id target-playlist-id))
              (do
                (println "")
                (println "🎉 Workflow completed successfully!"))
              (do
                (println "")
                (println "⚠️  Categorization succeeded but track operation failed")))
            (do
              (println "")
              (println "❌ Workflow failed at categorization step"))))
        (do
          (println "❌ No track currently playing or failed to get track ID")
          (System/exit 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*)) 