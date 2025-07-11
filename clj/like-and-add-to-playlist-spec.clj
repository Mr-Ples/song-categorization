#!/usr/bin/env bb

(require '[babashka.process :as process]
         '[clojure.string :as str]
         '[lambdaisland.dotenv :as dotenv])

(defn display-usage []
  (println "Usage: bb like-uncategorized.clj [OPTIONS]")
  (println "")
  (println "Like the currently playing Spotify track and add it to the uncategorized playlist.")
  (println "Uses SPOTIFY_UNCATEGORIZED_PLAYLIST_ID from .env file.")
  (println "")
  (println "Options:")
  (println "  --quiet      Suppress output (success/error codes only)")
  (println "  --help, -h   Show this help message"))

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
          playlist-id (get env-vars "SPOTIFY_UNCATEGORIZED_PLAYLIST_ID")]
      
      (when (nil? playlist-id)
        (when-not quiet?
          (println "❌ Error: SPOTIFY_UNCATEGORIZED_PLAYLIST_ID not found in .env file"))
        (System/exit 1))
      
      (when-not quiet?
        (println "🎵 Like and Add to Uncategorized Playlist")
        (println "========================================")
        (println (str "Uncategorized Playlist ID: " playlist-id))
        (println ""))
      
      ;; Call the main like-and-add-to-playlist script
      (let [result (if quiet?
                     (process/shell {:inherit true} "bb" "like-and-add-to-playlist.clj" "--quiet" playlist-id)
                     (process/shell {:inherit true} "bb" "like-and-add-to-playlist.clj" playlist-id))]
        (System/exit (:exit result))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*))
