# rfr-e2e

UI tests: JUnit 6 + Playwright Java + Allure 3. Design notes, structure and coverage: `README.md` (Russian).

```bash
./gradlew e2e                                                                     # all tests
./gradlew :rfr-e2e:test --tests 'io.student.rangiffler.test.profile.ProfileTest'   # one class
./gradlew :rfr-e2e:test --tests '*ProfileTest.avatarShouldBeSaved'                # one test
./gradlew e2e -Drfr.headless=false                                                # visible browser
./gradlew :rfr-e2e:allureServe   # Allure report (allureReport to just generate)
```

## Running
- Start the whole stand first (root `CLAUDE.md`): tests use the real services and both DBs.
- Run tests through Gradle: it attaches the AspectJ agent that Allure `@Step` needs.
- Call a test working only after a Gradle run passes; walking the scenario through Playwright MCP is not a test run.
- Expect the first run to be slow: Playwright downloads Chromium.
- Keep parallel execution off (`src/test/resources/junit-platform.properties`) unless asked to enable it.

## Writing tests
- Annotate UI test classes with `@WebTest` and take `Page` as a test parameter; don't create Playwright yourself. Keep the browser locale `en-US`: rfr-auth localizes messages by `Accept-Language`.
- Get users from a `@User TestUser` parameter (random credentials; set `username`/`password` in the annotation when needed). Don't create them through the UI unless registration is under test; cleanup in both DBs is automatic.
- Log in only through the UI; never put tokens into localStorage.
- Create and delete test data only through fixtures; never use broad `DELETE`/`TRUNCATE` — other tests' data lives in the same DBs.
- Read stand settings only through `TestConfig`; override with `-Drfr.<key>` or `RFR_<KEY>`.
- Put SQL in `src/test/resources/sql/{auth,api}/` and run it through the JDBC repositories in `data/`. When a new table references users, add its delete script to `ApiUserRepository`'s cleanup list before the user's own: the schema has no cascading foreign keys.
- Write Page Objects and organize tests by the `rfr-page-objects` skill (`.claude/skills/rfr-page-objects/SKILL.md`).
