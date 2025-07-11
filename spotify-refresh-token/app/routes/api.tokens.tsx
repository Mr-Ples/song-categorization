import type { Route } from "./+types/api.tokens";

export async function loader({ request }: Route.LoaderArgs) {
  const url = new URL(request.url);
  const clientId = url.searchParams.get("client_id");
  const clientSecret = url.searchParams.get("client_secret");

  if (!clientId || !clientSecret) {
    return Response.json(
      { error: "Missing client_id or client_secret parameters" },
      { status: 400 }
    );
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

  const redirectUri = "https://spotify-refresh-token-app.sidenotes.workers.dev/callback";
  
  const authUrl = `https://accounts.spotify.com/authorize?${new URLSearchParams({
    client_id: clientId,
    response_type: "code",
    redirect_uri: redirectUri,
    scope: scopes,
    state: btoa(JSON.stringify({ clientId, clientSecret }))
  }).toString()}`;

  return Response.json({
    message: "Navigate to this URL to authorize and get tokens",
    authUrl,
    redirectUri,
    scopes: scopes.split(" ")
  });
}

export async function action({ request }: Route.ActionArgs) {
  const formData = await request.formData();
  const code = formData.get("code") as string;
  const clientId = formData.get("client_id") as string;
  const clientSecret = formData.get("client_secret") as string;

  if (!code || !clientId || !clientSecret) {
    return Response.json(
      { error: "Missing required parameters: code, client_id, client_secret" },
      { status: 400 }
    );
  }

  try {
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
      const error = await tokenResponse.text();
      return Response.json(
        { error: "Failed to exchange code for tokens", details: error },
        { status: 400 }
      );
    }

    const tokens = await tokenResponse.json();
    
    return Response.json({
      access_token: tokens.access_token,
      refresh_token: tokens.refresh_token,
      expires_in: tokens.expires_in,
      token_type: tokens.token_type,
      scope: tokens.scope
    });

  } catch (error) {
    return Response.json(
      { error: "Internal server error", details: error instanceof Error ? error.message : "Unknown error" },
      { status: 500 }
    );
  }
}
