# rfr-e2e

```bash
./gradlew :rfr-e2e:test --tests '*ProfileTest.avatarShouldBeSaved'   # one test
./gradlew e2e -Drfr.headless=false                                   # visible browser
./gradlew :rfr-e2e:allureServe                                       # Allure report
```

## Running
- Run tests through Gradle: it attaches the AspectJ agent that Allure `@Step` needs.
- Call a test working only after a Gradle run passes; walking the scenario through Playwright MCP is not a test run.
- Run `./gradlew :rfr-e2e:installPlaywright` once on a new machine; otherwise the first test downloads all three browsers. In CI use `installPlaywrightWithDeps`: Ubuntu runners lack Chromium's system libraries.
- Keep parallel execution off (`src/test/resources/junit-platform.properties`) unless asked to enable it.

## Writing tests
- Annotate UI test classes with `@WebTest` and take `Page` as a test parameter; don't create Playwright yourself. Keep the browser locale `en-US`: rfr-auth localizes messages by `Accept-Language`.
- Get users from a `@User TestUser` parameter. Don't create them through the UI unless registration is under test; cleanup in both DBs is automatic.
- Log in only through the UI; never put tokens into localStorage.
- Create and delete test data only through fixtures; never use broad `DELETE`/`TRUNCATE`: other tests' data lives in the same DBs.
- Read stand settings only through `TestConfig`; override with `-Drfr.<key>` or `RFR_<KEY>`.
- Put all SQL for test data into immutable records in `data/db` (`AuthAccount`, `Profile`, `Friendship`, `Friendships`) with `create()` / `delete()` / `exists()`; fixtures call only these records.
- When a new table references users, add its delete script to `Profile`'s `DELETE_CHILDREN_SQL`: the schema has no cascading foreign keys.
- Test extensions with EngineTestKit: put the probe classes it runs inside the test class as `static` nested classes annotated `@ProbeTarget` (skipped unless the outer test's `run()` sets `ProbeTargetCondition.RUN_PROBES`; Gradle also excludes its tag), report facts with `TestReporter`, and check the DB in the outer test after the probe's store has closed.
