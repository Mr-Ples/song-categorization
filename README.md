# Song Categorization

This is an attempt to create a sparse interpretable dataset that represents the actual latent space that constitutes my intuitive categorization of music and to use it to create a playlist generator and ultimately a recommender system.

Additionally it's an investigation into music, culture, philosophy of programs and cetagorization

## Usage

Ultimately this is a tool for categorization, the idea is to have a uncategorized playlist that you can enrich with your liked playlist or whatever other source you want, and an empty categorized playlist

After you categorize a song via the tooling below it moves from the uncategorized playlist to the categorized playlist

### everything is a util
- every script is a standalone general usage functional program, there is no tight coupling or monolithic program
- this decoupling reduces stress because you can play with any part of the system without fucking up the whole
- every script has a `-h` or `--help` arg

### Setup

1) create an `.env`:
```bash
cp .env.example .env
```

2) get your client id and secret from the spotify developer dashboard (you might have to create an app)
3) get your access token and refresh token either by deploying the web app (see `./spotify-refresh-token/README.md` for deployment details, its free and easy and fast, literally 2 commands or just use my deployement - it's a web app that is both UI and API for generating access/refresh tokens and an API https://spotify-refresh-token-app.sidenotes.workers.dev/, you would need to add this URL as your redirect URI in the developer dashboard)
4) create an `UNCATEGORIZED` playlist in spotify and get its id (copy the playlist link into a browser and get the id from the url, `.../playlist/<playlist-id>`)
5) create a `CATEGORIZED` playlist in spotify and get its id (copy the playlist link into a browser and get the id from the url, `.../playlist/<playlist-id>`)
6) you need babashka to run clojure scripts, install it via https://github.com/babashka/babashka#installation

### Example usage

1) when i encounter a song i like i just have this script keybinded on my system to like it and move it to my `UNCATEGORIZED` playlist (and like it)
```bash
cd <this-repo>/clj && bb like-and-add-to-playlist-spec.clj 
```

2) to categorize a song i have this bind:
```bash
cd <this-repo>/clj && bb categorize-and-move-spec.clj
```

A new categorization file is created in `clj/data`, (see `clj/example_categorization.json` for an example output)

---

## Future
- train a recommender model
- see what data sets exists for tracks
- create my own track dataset if needed (maybe lyrics + song metadata + maybe even raw tokenized audio is enough)
- categorize all songs again and compare the results (drift would be interesting)
- generate playlists by slicing along a subset of dimensions and see if it aligns with intuitive categories
- create a similar category system for personal mood and see if it correlates with drift
- create a similar category system for sub cultures and see if there are correlations with categorized songs
- use model to find regularities
    - e.g. like high energy / not repeatible / hits the spot cluster 
    - e.g. this cluster could correlate with other systems e.g. dopamine neuromodulation e.g. specific sub cultures
    - e.g. songs you have to endure to enjoy are more relistable than songs that hit the spot
    - e.g. this correlates with serotonin neuromodulation
    - e.g. what subset of (in distrubition but also out of distrobution) dimensions define adoption of a unit of music (genre/band/song/other) to a sub culture?

## Notes

`doc/NOTES.md`
