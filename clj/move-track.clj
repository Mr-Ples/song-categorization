#!/usr/bin/env bb

(require '[babashka.curl :as curl]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.string :as str]
         '[lambdaisland.dotenv :as dotenv])

(defn get-access-token []
  (str/trim (:out (process/shell {:out :string} "bb" "get-access-token.clj"))))

(defn remove-track-from-playlist [access-token playlist-id track-id]
  (try
    (let [track-uri (str "spotify:track:" track-id)
          body {:tracks [{:uri track-uri}]}
          response (curl/delete (str "https://api.spotify.com/v1/playlists/" playlist-id "/tracks")
                               {:headers {"Authorization" (str "Bearer " access-token)
                                        "Content-Type" "application/json"}
                                :body (json/generate-string body)})]
      (when (= 200 (:status response))
        (let [result (json/parse-string (:body response) true)]
          (:snapshot_id result))))
    (catch Exception e
      (println "Error removing track from playlist:" (.getMessage e))
      nil)))

(defn add-track-to-playlist [access-token playlist-id track-id]
  (try
    (let [track-uri (str "spotify:track:" track-id)
          body {:uris [track-uri]}
          response (curl/post (str "https://api.spotify.com/v1/playlists/" playlist-id "/tracks")
                             {:headers {"Authorization" (str "Bearer " access-token)
                                      "Content-Type" "application/json"}
                              :body (json/generate-string body)})]
      (when (= 201 (:status response))
        (let [result (json/parse-string (:body response) true)]
          (:snapshot_id result))))
    (catch Exception e
      (println "Error adding track to playlist:" (.getMessage e))
      nil)))

(defn get-track-info [access-token track-id]
  (try
    (let [response (curl/get (str "https://api.spotify.com/v1/tracks/" track-id)
                            {:headers {"Authorization" (str "Bearer " access-token)}})]
      (when (= 200 (:status response))
        (json/parse-string (:body response) true)))
    (catch Exception e
      (println "Error getting track info:" (.getMessage e))
      nil)))

(defn display-usage []
  (println "Usage: bb move-track.clj [OPTIONS] <TRACK_ID> <TARGET_PLAYLIST_ID>")
  (println "       bb move-track.clj [OPTIONS] <TRACK_ID> <SOURCE_PLAYLIST_ID> <TARGET_PLAYLIST_ID>")
  (println "")
  (println "Add a track to a playlist (2 args) or move between playlists (3 args).")
  (println "When moving, the track is first added to target, then removed from source.")
  (println "")
  (println "Options:")
  (println "  --help, -h   Show this help message")
  (println "")
  (println "Arguments:")
  (println "  TRACK_ID              Spotify track ID (from track URI or URL)")
  (println "  TARGET_PLAYLIST_ID    Spotify playlist ID to add track to")
  (println "  SOURCE_PLAYLIST_ID    Spotify playlist ID to remove track from (move only)")
  (println "")
  (println "Examples:")
  (println "  # Add track to playlist")
  (println "  bb move-track.clj 4iV5W9uYEdYUVa79Axb7Rh 6wsBrS9qpdSA5kSnwdJM3g")
  (println "  # Move track between playlists") 
  (println "  bb move-track.clj 4iV5W9uYEdYUVa79Axb7Rh 1it5Tbzz9tJ4464wp6oq5N 6wsBrS9qpdSA5kSnwdJM3g"))

(defn main [& args]
  (when (some #{"--help" "-h"} args)
    (display-usage)
    (System/exit 0))
  
  (when (not (or (= 2 (count args)) (= 3 (count args))))
    (display-usage)
    (System/exit 1))
  
  (let [track-id (first args)
        is-move-operation (= 3 (count args))
        target-playlist-id (if is-move-operation (nth args 2) (second args))
        source-playlist-id (when is-move-operation (second args))]
    
    (println "🎵 Moving track between playlists...")
    (println (str "   Track ID: " track-id))
    (when source-playlist-id
      (println (str "   Source Playlist: " source-playlist-id)))
    (println (str "   Target Playlist: " target-playlist-id))
    (println "")
    
    (let [access-token (get-access-token)]
      (when (empty? access-token)
        (println "❌ Failed to get access token")
        (System/exit 1))
      
      ;; Get track info for display
      (let [track-info (get-track-info access-token track-id)]
        (if track-info
          (let [track-name (:name track-info)
                artist-name (get-in track-info [:artists 0 :name])]
            (println (str "🎶 Track: " track-name " - " artist-name))
            (println ""))
          (println "⚠️  Could not retrieve track info, but proceeding with move...")))
      
             ;; Step 1: Add to target playlist first
       (println "📥 Adding track to target playlist...")
       (let [add-result (add-track-to-playlist access-token target-playlist-id track-id)]
         (if add-result
           (println (str "✅ Successfully added to target playlist (snapshot: " add-result ")"))
           (do
             (println "❌ Failed to add track to target playlist")
             (System/exit 1))))
       
       ;; Step 2: Remove from source playlist (only if this is a move operation)
       (if is-move-operation
         (do
           (println "📤 Removing track from source playlist...")
           (let [remove-result (remove-track-from-playlist access-token source-playlist-id track-id)]
             (if remove-result
               (do
                 (println (str "✅ Successfully removed from source playlist (snapshot: " remove-result ")"))
                 (println "")
                 (println "🎉 Track moved successfully!"))
               (do
                 (println "❌ Failed to remove track from source playlist")
                 (println "⚠️  Note: Track was successfully added to target playlist but not removed from source")
                 (System/exit 1)))))
         (do
           (println "")
           (println "🎉 Track added successfully!")))
       )))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*)) 