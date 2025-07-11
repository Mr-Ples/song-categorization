import { type RouteConfig, index, route } from "@react-router/dev/routes";

export default [
  index("routes/home.tsx"),
  route("callback", "routes/callback.tsx"),
  route("api/tokens", "routes/api.tokens.tsx"),
  route("api/refresh", "routes/api.refresh.tsx"),
] satisfies RouteConfig;
