# rfr-auth

## Tokens
- Look for custom claims only in the access token: `id_token` has `aud: client` and serves only as `id_token_hint` on logout.
- Don't rely on anything in the authorization server surviving a restart: the RSA key and OAuth authorizations live in memory.

## Endpoints
- Start every login flow, tests included, from `/oauth2/authorize` or the frontend Login button: a bare `GET /login` with no saved OAuth request redirects to the frontend.
- Keep `PublicClientRevocationAuthentication{Converter,Provider}` limited to `/oauth2/revoke`; don't relax client authentication on other endpoints.

## Registration
- To register over HTTP, `GET /register` first and post the form with its CSRF token in the same session. Expect 201 on success and 400 with the same form on a validation error or taken username.
- Expect U+00A0 to pass the no-whitespace check: it uses `Character.isWhitespace`.
