#!/usr/bin/env bb

(require '[babashka.process :as process]
         '[clojure.string :as str]
         '[clojure.edn :as edn])

(defn display-usage []
  (println "Usage: bb like-and-add-to-playlist.clj [OPTIONS] <PLAYLIST_ID>")
  (println "")
  (println "Like the currently playing Spotify track and add it to the specified playlist.")
  (println "")
  (println "Options:")
  (println "  --quiet      Suppress output (success/error codes only)")
  (println "  --help, -h   Show this help message")
  (println "")
  (println "Arguments:")
  (println "  PLAYLIST_ID   Spotify playlist ID to add track to")
  (println "")
  (println "Examples:")
  (println "  bb like-and-add-to-playlist.clj 6wsBrS9qpdSA5kSnwdJM3g")
  (println "  bb like-and-add-to-playlist.clj --quiet 6wsBrS9qpdSA5kSnwdJM3g"))

(defn get-current-track-id []
  (let [result (process/shell {:out :string :continue true} "bb" "get-current-track.clj" "--edn")]
    (if (= 0 (:exit result))
      (let [track-data (edn/read-string (str/trim (:out result)))]
        (if (:error track-data)
          nil
          (:id track-data)))
      nil)))

(defn like-current-track [quiet?]
  "Run the like-current-track script"
  (let [result (if quiet?
                 (process/shell {:out :string :err :string :continue true} "bb" "like-current-track.clj" "--quiet")
                 (process/shell {:out :string :err :string :continue true} "bb" "like-current-track.clj"))]
    (= 0 (:exit result))))

(defn add-track-to-playlist [track-id playlist-id quiet?]
  "Use move-track.clj to add track to playlist"
  (let [result (if quiet?
                 (process/shell {:out :string :err :string :continue true} "bb" "move-track.clj" track-id playlist-id)
                 (process/shell {:continue true} "bb" "move-track.clj" track-id playlist-id))]
    (= 0 (:exit result))))

(defn main [& args]
  (when (some #{"--help" "-h"} args)
    (display-usage)
    (System/exit 0))
  
  (let [non-flag-args (filter #(not (str/starts-with? % "--")) args)
        quiet? (some #{"--quiet"} args)]
    
    (when (not= 1 (count non-flag-args))
      (display-usage)
      (System/exit 1))
    
    (let [playlist-id (first non-flag-args)]
      
      (when-not quiet?
        (println "🎵 Like and Add to Playlist Workflow")
        (println "====================================")
        (println (str "Target Playlist: " playlist-id))
        (println ""))
      
      ;; Step 1: Like the current track
      (when-not quiet?
        (println "❤️ Liking current track..."))
      (if (like-current-track quiet?)
        (when-not quiet?
          (println "✅ Track liked successfully"))
        (do
          (when-not quiet?
            (println "❌ Failed to like track"))
          (System/exit 1)))
      
      ;; Step 2: Get current track ID
      (let [track-id (get-current-track-id)]
        (if track-id
          (do
            (when-not quiet?
              (println (str "🎶 Current Track ID: " track-id))
              (println ""))
            
            ;; Step 3: Add track to playlist using move-track.clj
            (if (add-track-to-playlist track-id playlist-id quiet?)
              (when-not quiet?
                (println "")
                (println "🎉 Workflow completed successfully!"))
              (do
                (when-not quiet?
                  (println "❌ Failed to add track to playlist"))
                (System/exit 1))))
          (do
            (when-not quiet?
              (println "❌ No track currently playing or failed to get track ID"))
            (System/exit 1)))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*))
