#!/usr/bin/env bb

(require '[babashka.curl :as curl]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.string :as str])

(defn display-usage []
  (println "Usage: bb like-current-track.clj [OPTIONS]")
  (println "")
  (println "Like the currently playing Spotify track.")
  (println "")
  (println "Options:")
  (println "  --quiet      Suppress output (success/error codes only)")
  (println "  --help, -h   Show this help message")
  (println "")
  (println "Examples:")
  (println "  bb like-current-track.clj")
  (println "  bb like-current-track.clj --quiet"))

(defn get-access-token []
  (str/trim (:out (process/shell {:out :string} "bb" "get-access-token.clj"))))

(defn get-current-track [access-token]
  (try
    (let [response (curl/get "https://api.spotify.com/v1/me/player/currently-playing"
                            {:headers {"Authorization" (str "Bearer " access-token)}})]
      (when (= 200 (:status response))
        (json/parse-string (:body response) true)))
    (catch Exception e
      (println "Error getting current track:" (.getMessage e))
      nil)))

(defn like-track [access-token track-id]
  (try
    (let [response (curl/put (str "https://api.spotify.com/v1/me/tracks?ids=" track-id)
                            {:headers {"Authorization" (str "Bearer " access-token)
                                     "Content-Type" "application/json"}})]
      (= 200 (:status response)))
    (catch Exception e
      (println "Error liking track:" (.getMessage e))
      false)))

(defn main [& args]
  (when (some #{"--help" "-h"} args)
    (display-usage)
    (System/exit 0))
  
  (let [quiet? (some #{"--quiet"} args)
        access-token (get-access-token)]
    
    (when (empty? access-token)
      (when-not quiet?
        (println "❌ Failed to get access token"))
      (System/exit 1))
    
    (let [track-info (get-current-track access-token)]
      (if track-info
        (let [track-id (get-in track-info [:item :id])
              track-name (get-in track-info [:item :name])
              artist-name (get-in track-info [:item :artists 0 :name])]
          (if (like-track access-token track-id)
            (when-not quiet?
              (println (str "❤️ Liked: " track-name " - " artist-name)))
            (do
              (when-not quiet?
                (println "❌ Failed to like track"))
              (System/exit 1))))
        (do
          (when-not quiet?
            (println "⏸️ No track currently playing"))
          (System/exit 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*)) 