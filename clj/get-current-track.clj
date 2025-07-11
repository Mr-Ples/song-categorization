#!/usr/bin/env bb

(require '[babashka.curl :as curl]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.string :as str]
         '[lambdaisland.dotenv :as dotenv])

(defn display-usage []
  (println "Usage: bb get-current-track.clj [OUTPUT_FORMAT]")
  (println "")
  (println "Get information about the currently playing Spotify track.")
  (println "")
  (println "Output formats:")
  (println "  (no args)    Human-readable format with track details")
  (println "  --edn        EDN format for programmatic use")
  (println "  --json       Full JSON response from Spotify API")
  (println "  --id         Track ID only")
  (println "")
  (println "Examples:")
  (println "  bb get-current-track.clj")
  (println "  bb get-current-track.clj --edn")
  (println "  bb get-current-track.clj --id"))

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

(defn spotify-track->record [track]
  {:id (:id track)
   :name (:name track)
   :artist (get-in track [:artists 0 :name])
   :album (get-in track [:album :name])
   :uri (:uri track)
   :external-url (get-in track [:external_urls :spotify])
   :duration-ms (:duration_ms track)})

(defn display-track-info [track-record]
  (println (str "\n🎵 Currently playing: " (:name track-record) " - " (:artist track-record)))
  (println (str "   Album: " (:album track-record)))
  (println (str "   Track ID: " (:id track-record))))

(defn main [& args]
  (when (some #{"--help" "-h"} args)
    (display-usage)
    (System/exit 0))
  
  (let [access-token (get-access-token)
        output-format (first args)]
    (when (empty? access-token)
      (if (= output-format "--edn")
        (do (println "{:error :auth-failed}")
            (System/exit 1))
        (do (println "❌ Failed to get access token")
            (System/exit 1))))
    
    (let [track-info (get-current-track access-token)]
      (if track-info
        (let [track-record (spotify-track->record (:item track-info))]
          (case output-format
            "--edn" (println (pr-str track-record))
            "--json" (println (json/generate-string (:item track-info) {:pretty true}))
            "--id" (println (:id track-record))
            ;; Default: human-readable
            (display-track-info track-record)))
        (case output-format
          "--edn" (println "{:error :no-track}")
          "--id" (System/exit 1)
          (println "⏸️ No track currently playing"))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply main *command-line-args*))