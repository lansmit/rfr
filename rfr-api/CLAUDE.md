# rfr-api

## Schema and codegen
- Don't change the shape of `src/main/resources/graphql/query.graphqls`: the frontend is built against it.
- Don't hand-write GraphQL types (`io.student.rangiffler.model.types.*`): DGS Codegen generates them on compile.
- Ignore IDEA marking `*Connection` types red: they aren't in the schema, Spring GraphQL synthesizes them from `Page`/`Slice` returns. If a service starts returning `Slice`, switch `typeMapping` in `build.gradle`.

## Controllers
- Put `@PreAuthorize("hasAuthority('read')")` on every query controller and `@PreAuthorize("hasAuthority('write')")` on every mutation controller: the filter chain permits all `POST /graphql`, so the annotation is the only check.
- Take paging args as `@Nullable Integer`: clients send explicit `null`.
- Never call `@SchemaMapping` resolver methods directly; let Spring GraphQL resolve nested fields.
- Add every new exception type to `GraphQlExceptionResolver`.

## Mocks
- Keep `FeedMockQueryController`, `PhotoMockMutationController` and `FeedMockData` until the user explicitly asks to replace them; tables `photo`, `like`, `photo_like`, `statistic` already exist in the migration.
- Split `V1__schema_init.sql` per service when extracting microservices: it is written as one monolithic DB.

## Data layer
- Model new relations after friendship (`FriendshipEntity`): own aggregate, pair uniqueness enforced in the DB, transition rules on the entity, `@Version`.
- Flush inside the service (`saveAndFlush` / `flush()`) so constraint and lock exceptions are caught there and turned into a domain exception, not thrown at commit.
- Fetch lists in one paginated query taking `Pageable` (projection into a record, joins included), never from entity collections: no N+1.
- Check ownership on every mutation: editing or deleting someone else's photo, liking twice.
- After changing a `@Query`, start rfr-api (needs MySQL) to validate it: a green `./gradlew build` says nothing about queries.
