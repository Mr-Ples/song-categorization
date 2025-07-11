#!/usr/bin/env bb

(require '[babashka.process :as process]
         '[clojure.string :as str]
         '[lambdaisland.dotenv :as dotenv])

(defn display-usage []
  (println "Usage: bb categorize-uncategorized.clj [OPTIONS]")
  (println "")
  (println "Categorize the currently playing track and move it from uncategorized to a target playlist.")
  (println "Uses SPOTIFY_UNCATEGORIZED_PLAYLIST_ID and SPOTIFY_CATEGORIZED_PLAYLIST_ID from .env file.")
  (println "")
  (println "Options:")
  (println "  --quiet      Suppress output (success/error codes only)")
  (println "  --help, -h   Show this help message")
  (println "")
  (println "Environment variables:")
  (println "  SPOTIFY_UNCATEGORIZED_PLAYLIST_ID  Source playlist (uncategorized)")
  (println "  SPOTIFY_CATEGORIZED_PLAYLIST_ID         Target playlist for categorized tracks")
  (println "")
  (println "This script will:")
  (println "1. Run categorization on the current track")
  (println "2. Move track from uncategorized to target playlist"))

(defn main [& args]
  (when (some #{"--help" "-h"} args)
    (display-usage)
    (System/exit 0))
  
  (let [quiet? (some #{"--quiet"} args)]
    
    ;; Load .env file
    (let [env-vars (try
                     (dotenv/parse-dotenv (slurp ".env"))
                     (catch Exception e
                       (when-not quiet?
                         (println "❌ Error: Could not read .env file"))
                       (System/exit 1)))
          uncategorized-playlist-id (get env-vars "SPOTIFY_UNCATEGORIZED_PLAYLIST_ID")
          target-playlist-id (get env-vars "SPOTIFY_CATEGORIZED_PLAYLIST_ID")]
      
      (when (nil? uncategorized-playlist-id)
        (when-not quiet?
          (println "❌ Error: SPOTIFY_UNCATEGORIZED_PLAYLIST_ID not found in .env file"))
        (System/exit 1))
      
      (when (nil? target-playlist-id)
        (when-not quiet?
          (println "❌ Error: SPOTIFY_CATEGORIZED_PLAYLIST_ID not found in .env file"))
        (System/exit 1))
      
      (when-not quiet?
        (println "🎵 Categorize and Move from Uncategorized")
        (println "=========================================")
        (println (str "Source Playlist (Uncategorized): " uncategorized-playlist-id))
        (println (str "Target Playlist: " target-playlist-id))
        (println ""))
      
      ;; Call the main categorize-and-move script
      (let [result (if quiet?
                     (process/shell {:inherit true} "bb" "categorize-and-move.clj" uncategorized-playlist-id target-playlist-id)
                     (process/shell {:inherit true} "bb" "categorize-and-move.clj" uncategorized-playlist-id target-playlist-id))]
        (System/exit (:exit result))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*))