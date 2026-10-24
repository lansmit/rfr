# rfr-api

GraphQL API (Spring GraphQL), OAuth2 resource server, JPA + Flyway. Port 8081 (`/graphql`, `/graphiql`). Design notes: `README.md` (Russian). Auth, error and mock rules: root `CLAUDE.md`.

## Schema and codegen
- Don't change the shape of `src/main/resources/graphql/query.graphqls`: the frontend is built against it.
- Don't hand-write GraphQL types (`io.student.rangiffler.model.types.*`): DGS Codegen generates them into `build/generated/sources/dgs-codegen` (`./gradlew :rfr-api:generateJava`, also runs on compile).
- Ignore IDEA marking `*Connection` types red: they aren't in the schema, Spring GraphQL synthesizes them from `Page`/`Slice` returns. If a service starts returning `Slice`, switch `typeMapping` in `build.gradle` (now `UserConnection`/`PhotoConnection` → `Page<...>`, `Date` → `LocalDate`).

## Controllers
- Put `@PreAuthorize("hasAuthority('read')")` on every query controller and `@PreAuthorize("hasAuthority('write')")` on every mutation controller: the filter chain permits all `POST /graphql`, so the annotation is the only check.
- Get the current user from `@AuthenticationPrincipal Jwt` (`sub`).
- Name arguments explicitly (`@Argument("name")`) and take paging args as `@Nullable Integer`: clients send explicit `null`.
- Resolve nested fields (`User.friends`, `Feed.photos`, `Photo.likes`, …) with `@SchemaMapping`; never call those resolver methods directly.
- Throw `ResourceNotFoundException`, `IllegalArgumentException` or a domain `*ActionException` so `GraphQlExceptionResolver` maps them; add new exception types to the resolver.

## Replacing the mocks
- Replace `FeedMockQueryController`, `PhotoMockMutationController` and `FeedMockData` (`resources/mock/*.json`) with real services; tables `photo`, `like`, `photo_like`, `statistic` already exist in the migration.
- Split `V1__schema_init.sql` per service when extracting microservices: it is written as one monolithic DB.

## Data layer
- Model new relations like friendship: a separate aggregate (`FriendshipEntity` + `FriendshipRepository`), one row per user pair, pair uniqueness enforced in the DB (generated columns `user_low_id`/`user_high_id`), transition rules on the entity (`request`, `accept(actor)`, `assertCanDecline(actor)`), `@Version` for optimistic locking.
- Flush inside the service (`saveAndFlush` / `flush()`) so constraint and lock exceptions are caught there and turned into `FriendshipActionException`, not thrown at commit.
- Fetch lists of people in one paginated JPQL query into a record (`select new ... UserWithStatus`) joined with country — no N+1. Write each query in with-search and without-search variants and pick one in the service. Derive `FriendStatus` from `friendshipStatus` + `isRequester`.
- Paginate only with a single repository query taking `Pageable`, never from entity collections. Use projections for computed fields like `isOwner`.
- Use `java.time`, not `java.util.Date`.
- Check ownership on every mutation: editing or deleting someone else's photo, liking twice.
- After changing a `@Query`, start rfr-api (needs MySQL) to validate it: a green `./gradlew build` says nothing about queries.
