# rfr-gql-client

- Change the app only when asked: it is the system under test, not a work item.
- Install with `npm ci`, not `npm i`: newer npm rewrites `package-lock.json` with `"peer": true` lines.
- Match GraphQL requests in tests by `operationName` (one hook per operation in `src/hooks/`): every operation goes to the same `/graphql` URL.
- Never treat UI state right after a mutation as proof that it was saved: Apollo updates the card from the mutation response. Reload, or read through GraphQL or the DB.
- When changing the frontend port in `.env`, change the `client` redirect URIs in rfr-auth `application.yaml` to match.
