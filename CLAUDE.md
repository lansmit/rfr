# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Rangiffler — QA.GURU Advanced diploma scaffold: a travel photo app (map of visited countries, photo feed, likes, friends). The goal is to turn the monolithic `rfr-api` into microservices (gRPC suggested for inter-service calls) and to build a substantial e2e/API test suite in `rfr-e2e`. READMEs (root and per module) are in Russian and are the authoritative design notes.

Modules (Gradle multi-project, root `settings.gradle`: `rfr-api`, `rfr-auth`, `rfr-e2e`; the frontend is a separate npm project):

| Module | Role | Port | Module guide |
|---|---|---|---|
| `rfr-auth` | Spring Authorization Server (OAuth 2.1 / OIDC), Thymeleaf login/register pages | 9001 | [rfr-auth/CLAUDE.md](rfr-auth/CLAUDE.md) |
| `rfr-api` | GraphQL API, OAuth2 resource server, JPA + Flyway | 8081 (`/graphql`, `/graphiql`) | [rfr-api/CLAUDE.md](rfr-api/CLAUDE.md) |
| `rfr-gql-client` | React 19 + TS + Vite + Apollo + MUI SPA (finished, written against the fixed schema) | 3001 | [rfr-gql-client/CLAUDE.md](rfr-gql-client/CLAUDE.md) |
| `rfr-e2e` | JUnit 6 + Playwright Java + Allure 3 UI tests (auth, profile) | — | [rfr-e2e/CLAUDE.md](rfr-e2e/CLAUDE.md) |

Stack: Java 25 (Gradle toolchain), Gradle 9.7.1 wrapper, Spring Boot 4.1.1 (BOM applied from root `build.gradle` to api/auth), MySQL 8.4.

## Commands

```bash
./gradlew runAll                 # whole stand in the background (MySQL → auth → api → frontend), returns when all are up; logs in build/logs/
./gradlew stopAll                # stop it all (MySQL via docker compose down, data kept)

bash localenv.sh                 # MySQL via docker-compose.yml (root/secret, :3306), returns once the DB is healthy
bash ports.sh                    # kill whatever listens on 9001/8081/3001

./gradlew :rfr-auth:bootRun      # start auth first — rfr-api fetches JWKS from it
./gradlew :rfr-api:bootRun
./gradlew build                  # also runs :rfr-e2e:test, so it needs the stand; to only compile: :rfr-auth:build :rfr-api:build :rfr-e2e:testClasses

./gradlew e2e                    # all rfr-e2e tests; doesn't start the stand — run runAll first. Single test, headed mode, Allure: rfr-e2e/CLAUDE.md

cd rfr-gql-client && npm i && npm run dev
```

Never delete files: `rm` is denied. To drop a file, rename it to `<name>.bak` (`mv file file.bak`) and tell the user. `git push` is denied too — the user pushes.

Stop MySQL with `docker compose down`; never add `-v` without asking — it deletes the volume with both databases.

Put schema changes into a new Flyway migration under `src/main/resources/db/migration/<schema>/`; never edit an applied one (`ddl-auto: none`; databases are created and migrated on startup).

## Cross-module rules

### Auth
- Call rfr-api only with the access token (`Authorization: Bearer`): an `id_token` gets 401. Queries need the `read` authority, mutations `write`; the username is the JWT `sub`.
- After restarting rfr-auth, log in again before investigating any 401 from rfr-api: the signing key is regenerated on start.
- Don't expect revocation to lock a token out of rfr-api: it validates JWTs locally and accepts a revoked token until `exp`.
- Link users across `rangiffler-auth` and `rangiffler-api` by username only; their UUIDs differ. Expect no API profile until the user's first `Query.user` (first login).

### Errors
- Map exceptions in every service as `GraphQlExceptionResolver` does: `ResourceNotFoundException` → `NOT_FOUND`; `FriendshipActionException` and `IllegalArgumentException` → `BAD_REQUEST`; anything else → `INTERNAL_ERROR` without details. Respond HTTP 200 with `errors[]`.
- Put user-facing text only into `BAD_REQUEST`/`NOT_FOUND` messages: the frontend shows only those.
- In negative e2e tests assert on `extensions.classification` + `message`.

### Mocks
- Don't test persistence of feed, photos, likes or stats yet: they are mocks in rfr-api and save nothing. Users, countries and friendship are real.

### Java
- Import Jackson 3 (`tools.jackson.databind.*`), not `com.fasterxml.jackson.databind.*`: Spring Boot 4.
- Build through Gradle or name arguments explicitly: compilation relies on `-parameters`, and builds outside Gradle (e.g. VS Code `bin/main`) fail with `Name for argument of type [...] not specified`.

## Working with the agent

The IntelliJ IDEA MCP (`mcp__idea__*`) is optional: it comes from the user's IDE, not from `.mcp.json`. Use it when connected, otherwise fall back to the commands below. Read-only IDEA tools are allowed without prompts in `.claude/settings.json`.
- After editing Java files, check them with `get_file_problems` instead of a full build (fallback: `./gradlew :<module>:compileJava` / `compileTestJava`). Neither catches HQL/JPQL errors — those surface only when rfr-api starts.
- Look up classes and usages with `search_symbol` / `get_symbol_info` / `analyze_calls` rather than grepping and reading whole files; they also resolve the generated DGS types.
- For DB state (test data, what a fixture left behind) use the IDEA data source `rfr-mysql` (`localhost:3306`, both schemas) via `preview_table_data` / `execute_sql_query`; fallback: `docker exec rfr-mysql mysql -uroot -psecret <schema> -e "..."`.
- Inspect live pages with Playwright MCP (`.mcp.json`) when writing Page Objects; see the `rfr-page-objects` skill.
