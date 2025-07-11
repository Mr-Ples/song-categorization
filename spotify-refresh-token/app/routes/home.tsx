import { useState } from "react";
import type { Route } from "./+types/home";

export function meta({}: Route.MetaArgs) {
  return [
    { title: "Spotify Refresh Token Generator" },
    { name: "description", content: "Generate Spotify access and refresh tokens for your applications" },
  ];
}

export function loader({ context }: Route.LoaderArgs) {
  return { 
    redirectUri: "https://spotify-refresh-token-app.sidenotes.workers.dev/callback"
  };
}

export default function Home({ loaderData }: Route.ComponentProps) {
  const [clientId, setClientId] = useState("");
  const [clientSecret, setClientSecret] = useState("");
  const [tokens, setTokens] = useState<{access_token?: string; refresh_token?: string} | null>(null);
  const [loading, setLoading] = useState(false);

  const handleGenerateTokens = () => {
    if (!clientId || !clientSecret) {
      alert("Please enter both Client ID and Client Secret");
      return;
    }

    const scopes = [
      "user-read-private",
      "user-read-email", 
      "user-library-read",
      "user-library-modify",
      "playlist-read-private",
      "playlist-modify-public",
      "playlist-modify-private",
      "user-read-playback-state",
      "user-modify-playback-state",
      "user-read-currently-playing",
      "user-read-recently-played"
    ].join(" ");

    const params = new URLSearchParams({
      client_id: clientId,
      response_type: "code",
      redirect_uri: loaderData.redirectUri,
      scope: scopes,
      state: btoa(JSON.stringify({ clientId, clientSecret }))
    });

    window.location.href = `https://accounts.spotify.com/authorize?${params.toString()}`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-400 to-blue-600 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-xl p-8">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">
            Spotify Token Generator
          </h1>
          <p className="text-gray-600">
            Generate access and refresh tokens for your Spotify app
          </p>
        </div>

        {!tokens ? (
          <div className="space-y-6">
            <div>
              <label htmlFor="clientId" className="block text-sm font-medium text-gray-700 mb-2">
                Client ID
              </label>
              <input
                type="text"
                id="clientId"
                value={clientId}
                onChange={(e) => setClientId(e.target.value)}
                className="text-black w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent"
                placeholder="Enter your Spotify app Client ID"
              />
            </div>

            <div>
              <label htmlFor="clientSecret" className="block text-sm font-medium text-gray-700 mb-2">
                Client Secret
              </label>
              <input
                type="password"
                id="clientSecret"
                value={clientSecret}
                onChange={(e) => setClientSecret(e.target.value)}
                className="w-full text-black px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent"
                placeholder="Enter your Spotify app Client Secret"
              />
            </div>

            <button
              onClick={handleGenerateTokens}
              disabled={loading}
              className="w-full bg-green-500 hover:bg-green-600 disabled:bg-gray-400 text-black  font-semibold py-2 px-4 rounded-md transition duration-200"
            >
              {loading ? "Processing..." : "Generate Tokens"}
            </button>

            <div className="text-sm text-gray-600 space-y-2">
              <p><strong>Redirect URI:</strong> {loaderData.redirectUri}</p>
              <p className="text-xs">Make sure this redirect URI is added to your Spotify app settings.</p>
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="text-center">
              <h2 className="text-xl font-semibold text-green-600 mb-4">✅ Tokens Generated!</h2>
            </div>
            
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Access Token
              </label>
              <textarea
                readOnly
                value={tokens.access_token}
                className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-sm font-mono"
                rows={3}
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Refresh Token
              </label>
              <textarea
                readOnly
                value={tokens.refresh_token}
                className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-sm font-mono"
                rows={3}
              />
            </div>

            <button
              onClick={() => {
                setTokens(null);
                setClientId("");
                setClientSecret("");
              }}
              className="w-full bg-blue-500 hover:bg-blue-600 text-black  font-semibold py-2 px-4 rounded-md transition duration-200"
            >
              Generate New Tokens
            </button>
          </div>
        )}

        <div className="mt-8 text-center">
          <h3 className="text-sm font-medium text-gray-700 mb-2">API Usage</h3>
          <p className="text-xs text-gray-600">
            GET /api/tokens?client_id=YOUR_ID&client_secret=YOUR_SECRET
          </p>
        </div>
      </div>
    </div>
  );
}
