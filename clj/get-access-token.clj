#!/usr/bin/env bb

(require '[babashka.http-client :as http]
         '[cheshire.core :as json]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[lambdaisland.dotenv :as dotenv])

(defn display-usage []
  (println "Usage: bb get-access-token.clj [OPTIONS]")
  (println "")
  (println "Get a Spotify access token using the refresh token.")
  (println "Uses cached token if still valid, otherwise fetches a new one.")
  (println "")
  (println "Options:")
  (println "  --help, -h   Show this help message")
  (println "")
  (println "Environment variables required:")
  (println "  SPOTIFY_REFRESH_TOKEN    Your Spotify refresh token")
  (println "  SPOTIFY_CLIENT_ID        Your Spotify app client ID")
  (println "  SPOTIFY_CLIENT_SECRET    Your Spotify app client secret")
  (println "")
  (println "Examples:")
  (println "  bb get-access-token.clj")
  (println "  ACCESS_TOKEN=$(bb get-access-token.clj)"))

;; Check for help at the very start, before any other operations
(when (some #{"--help" "-h"} *command-line-args*)
  (display-usage)
  (System/exit 0))

;; Load and parse .env file
(def env-vars (dotenv/parse-dotenv (slurp ".env")))

;; Get specific variables
(def refresh-token (get env-vars "SPOTIFY_REFRESH_TOKEN"))
(def client-id (get env-vars "SPOTIFY_CLIENT_ID"))
(def client-secret (get env-vars "SPOTIFY_CLIENT_SECRET"))

;; Validate required environment variables
(when (or (nil? refresh-token) (nil? client-id) (nil? client-secret))
  (binding [*out* *err*]
    (println "Error: Missing required environment variables.")
    (println "Please ensure SPOTIFY_REFRESH_TOKEN, SPOTIFY_CLIENT_ID, and SPOTIFY_CLIENT_SECRET are set."))
  (System/exit 1))

;; Cache file location - in the same directory as this script
(def script-dir (.getParent (io/file *file*)))
(def cache-file (str script-dir "/.spotify_token_cache"))

(defn current-timestamp []
  (quot (System/currentTimeMillis) 1000))

(defn token-valid? []
  (when (.exists (io/file cache-file))
    (try
      (let [cached-data (slurp cache-file)
            [cached-token expiry-time-str] (str/split cached-data #"\|")
            expiry-time (Long/parseLong expiry-time-str)
            current-time (current-timestamp)
            buffer-time 300] ; 5 minute buffer
        (when (and cached-token 
                   expiry-time 
                   (< current-time (- expiry-time buffer-time)))
          cached-token))
      (catch Exception _
        nil))))

(defn fetch-and-cache-token! []
  (try
    (let [response (http/post "https://spotify-refresh-token-app.sidenotes.workers.dev/api/refresh"
                              {:form-params {:refresh_token refresh-token
                                           :client_id client-id
                                           :client_secret client-secret}
                               :throw false})
          body (json/parse-string (:body response) true)]
      
      (when (not= 200 (:status response))
        (binding [*out* *err*]
          (println "Error: Failed to make request to Spotify API")
          (println "Response:" response))
        (System/exit 1))
      
      (let [access-token (:access_token body)
            expires-in (or (:expires_in body) 3600)]
        
        (when (nil? access-token)
          (binding [*out* *err*]
            (println "Error: Failed to extract access token from response")
            (println "Response:" body))
          (System/exit 1))
        
        ;; Cache the token and expiry time
        (let [current-time (current-timestamp)
              expiry-time (+ current-time expires-in)
              cache-data (str access-token "|" expiry-time)]
          (spit cache-file cache-data)
          ;; Set file permissions to 600 (owner read/write only)
          (.setReadable (io/file cache-file) false false)
          (.setReadable (io/file cache-file) true true)
          (.setWritable (io/file cache-file) false false)
          (.setWritable (io/file cache-file) true true))
        
        access-token))
    (catch Exception e
      (binding [*out* *err*]
        (println "Error fetching token:" (.getMessage e)))
      (System/exit 1))))

;; Main logic: try to use cached token, otherwise fetch new one
(if-let [cached-token (token-valid?)]
  (println cached-token)
  (println (fetch-and-cache-token!))) 