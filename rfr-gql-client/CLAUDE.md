# rfr-gql-client

React 19 + TypeScript + Vite + Apollo Client 4 + MUI SPA. Port 3001. Finished and written against the fixed GraphQL schema; it is the system under test for rfr-e2e. Design notes: `README.md` (Russian).

```bash
npm ci            # use instead of npm i: newer npm rewrites package-lock.json with "peer": true lines
npm run dev       # http://localhost:3001, needs rfr-auth and rfr-api running
npm run build     # tsc + vite build
npm run lint      # eslint, --max-warnings 0
```

- Change the app only when asked: it is the system under test, not a work item.
- Match GraphQL requests in tests by `operationName` (`GetUser`, `GetFeed`, `GetPeople`, `UpdateUser`, `FriendshipAction`, `CreatePhoto`, `UpdatePhoto`, `LikePhoto`, `DeletePhoto`, …; one hook per operation in `src/hooks/`): every operation goes to the same `/graphql` URL.
- Never treat UI state right after a mutation as proof that it was saved: Apollo updates the card from the mutation response. Reload, or read through GraphQL or the DB.
- For manual GraphiQL/curl calls take the token from localStorage `access_token` (tests log in through the UI instead).
- Change URLs in `.env` (`VITE_AUTH_URL`, `VITE_API_URL`, `VITE_FRONT_URL`, …) and keep the port equal to the redirect URI registered in rfr-auth.
