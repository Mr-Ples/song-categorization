import type { Route } from "./+types/api.refresh";

export async function action({ request }: Route.ActionArgs) {
  const formData = await request.formData();
  const refreshToken = formData.get("refresh_token") as string;
  const clientId = formData.get("client_id") as string;
  const clientSecret = formData.get("client_secret") as string;

  if (!refreshToken || !clientId || !clientSecret) {
    return Response.json(
      { error: "Missing required parameters: refresh_token, client_id, client_secret" },
      { status: 400 }
    );
  }

  try {
    const response = await fetch("https://accounts.spotify.com/api/token", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "Authorization": `Basic ${btoa(`${clientId}:${clientSecret}`)}`
      },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: refreshToken
      })
    });

    if (!response.ok) {
      const error = await response.text();
      return Response.json(
        { error: "Failed to refresh token", details: error },
        { status: 400 }
      );
    }

    const tokens = await response.json();
    
    return Response.json({
      access_token: tokens.access_token,
      expires_in: tokens.expires_in,
      token_type: tokens.token_type,
      scope: tokens.scope,
      // Refresh token might not be returned if still valid
      ...(tokens.refresh_token && { refresh_token: tokens.refresh_token })
    });

  } catch (error) {
    return Response.json(
      { error: "Internal server error", details: error instanceof Error ? error.message : "Unknown error" },
      { status: 500 }
    );
  }
}
