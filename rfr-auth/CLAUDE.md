# rfr-auth

Spring Authorization Server (OAuth 2.1 / OIDC) with Thymeleaf login/register pages. Port 9001. Design notes: `README.md` (Russian). Cross-module auth rules: root `CLAUDE.md`.

## Client and tokens
- Configure the frontend client `client` in `application.yaml` (`spring.security.oauth2.authorizationserver.client.oidc-client`), not in code. Keep its redirect URIs on the frontend port 3001.
- Add access-token claims in `AccessTokenCustomizer` (it already sets `aud` from `rangiffler-api.audience` and `authorities`, dropping Spring Security 7 `FACTOR_*`). Don't look for them in `id_token`: that one has `aud: client` and serves only as `id_token_hint` on logout.
- Take the access-token TTL from `application.yaml` (now `30m`); the README's "10 minutes" is stale.
- Don't rely on anything in the authorization server surviving a restart: the RSA key (`jwkSource()`) and OAuth authorizations live in memory.

## Endpoints
- Start every login flow, tests included, from `/oauth2/authorize` or the frontend Login button: a bare `GET /login` with no saved OAuth request redirects to the frontend.
- Add OAuth/OIDC endpoint settings to the order-1 `SecurityFilterChain` and app pages to the order-2 one (`RangifflerAuthServiceConfig`).
- Keep `PublicClientRevocationAuthentication{Converter,Provider}` limited to `/oauth2/revoke`; don't relax client authentication on other endpoints.

## Registration
- To register over HTTP, `GET /register` first and post the form with its CSRF token in the same session. Expect 201 on success and 400 with the same form on a validation error or taken username.
- Validation (on `RegistrationForm`, `@EqualPasswords`, `@NoWhitespace`): username 3–50, password 3–12, no whitespace by `Character.isWhitespace` (U+00A0 passes), passwords equal.
- Registration writes only to `rangiffler-auth` (`user` + `authority` `read`/`write`, password `{bcrypt}`).
