import { redirect } from "react-router";
import type { Route } from "./+types/callback";

export async function loader({ request }: Route.LoaderArgs) {
  const url = new URL(request.url);
  const code = url.searchParams.get("code");
  const state = url.searchParams.get("state");
  const error = url.searchParams.get("error");

  if (error) {
    throw new Response("Authorization failed", { status: 400 });
  }

  if (!code || !state) {
    throw new Response("Missing authorization code or state", { status: 400 });
  }

  try {
    const { clientId, clientSecret } = JSON.parse(atob(state));
    
    // Exchange code for tokens
    const tokenResponse = await fetch("https://accounts.spotify.com/api/token", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "Authorization": `Basic ${btoa(`${clientId}:${clientSecret}`)}`
      },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        code,
        redirect_uri: "https://spotify-refresh-token-app.sidenotes.workers.dev/callback"
      })
    });

    if (!tokenResponse.ok) {
      throw new Error("Failed to exchange code for tokens");
    }

    const tokens = await tokenResponse.json();
    
    // Return the tokens as loader data instead of raw HTML
    return {
      tokens: {
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token,
        expires_in: tokens.expires_in
      }
    };

  } catch (error) {
    throw new Response("Failed to process callback", { status: 500 });
  }
}

export default function Callback({ loaderData }: Route.ComponentProps) {
  const { tokens } = loaderData;

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-400 to-blue-600 flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-white rounded-lg shadow-xl p-8">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-green-600 mb-2">✅ Success!</h1>
          <p className="text-gray-600">Your Spotify tokens have been generated</p>
        </div>
        
        <div className="space-y-4">
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
          
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Expires In
            </label>
            <input
              readOnly
              className="w-full px-3 py-2 border border-gray-300 rounded-md bg-gray-50 text-sm"
              value={`${tokens.expires_in} seconds`}
            />
          </div>
          
          <button
            onClick={() => window.location.href = '/'}
            className="w-full bg-blue-500 hover:bg-blue-600 text-black  font-semibold py-2 px-4 rounded-md transition duration-200"
          >
            Generate New Tokens
          </button>
        </div>
      </div>
    </div>
  );
}


