package io.student.rangiffler.test.extension;

import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.Friendships;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@DisplayName("UserExtension")
class UserExtensionTest {

  private static final String PROBE = "probe-target";
  private static final String CONFLICT_USERNAME = "e2e_probe_conflict";

  @Test
  @DisplayName("User exists in auth and API during the test and is deleted from both after it")
  void userShouldExistDuringTestAndBeDeletedAfterIt() {
    EngineExecutionResults results = run(SingleUserProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
    Map<String, String> facts = facts(results);
    assertEquals("true", facts.get("user.authExists"));
    assertEquals("true", facts.get("user.profileExists"));
    assertUserDeleted(facts.get("user.username"));
  }

  @Test
  @DisplayName("User is deleted after a failed test too")
  void userShouldBeDeletedAfterFailedTest() {
    EngineExecutionResults results = run(FailingTestProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
    assertUserDeleted(facts(results).get("user.username"));
  }

  @Test
  @DisplayName("Friends and income invitations exist during the test and are deleted with their users after it")
  void preconditionsShouldExistDuringTestAndBeDeletedAfterIt() {
    EngineExecutionResults results = run(PreconditionsProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
    Map<String, String> facts = facts(results);
    String username = facts.get("user.username");
    List<String> friends = list(facts.get("friends"));
    List<String> inviters = list(facts.get("inviters"));
    assertEquals(2, friends.size());
    assertEquals(1, inviters.size());
    assertEquals("true", facts.get("friendships.exist"));

    assertUserDeleted(username);
    assertFalse(new Friendships(username).exists(), "friendship rows left for " + username);
    friends.forEach(friend -> {
      assertFalse(Friendship.friends(friend, username).exists(), "friendship left with " + friend);
      assertUserDeleted(friend);
    });
    inviters.forEach(inviter -> {
      assertFalse(Friendship.invitation(inviter, username).exists(), "invitation left from " + inviter);
      assertUserDeleted(inviter);
    });
  }

  @Test
  @DisplayName("Each @User parameter gets its own user, and both are deleted after the test")
  void eachParameterShouldGetOwnUser() {
    EngineExecutionResults results = run(TwoUsersProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
    Map<String, String> facts = facts(results);
    assertNotEquals(facts.get("first.username"), facts.get("second.username"));
    assertUserDeleted(facts.get("first.username"));
    assertUserDeleted(facts.get("second.username"));
  }

  @Test
  @DisplayName("Auth account created before a failure in the middle of user creation is deleted")
  void partiallyCreatedUserShouldBeDeleted() {
    new Profile(CONFLICT_USERNAME).create();
    try {
      EngineExecutionResults results = run(CreationFailureProbe.class);

      results.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
      assertInstanceOf(ParameterResolutionException.class, failure(results));
      assertFalse(AuthAccount.of(CONFLICT_USERNAME, "").exists(), "auth account left for " + CONFLICT_USERNAME);
    } finally {
      new Profile(CONFLICT_USERNAME).delete();
    }
  }

  @Test
  @DisplayName("@User on a parameter that is not TestUser is not resolved")
  void unsupportedParameterTypeShouldNotBeResolved() {
    EngineExecutionResults results = run(UnsupportedTypeProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
    assertInstanceOf(ParameterResolutionException.class, failure(results));
  }

  private static EngineExecutionResults run(Class<?> probe) {
    return EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(probe))
        .execute();
  }

  private static Map<String, String> facts(EngineExecutionResults results) {
    Map<String, String> facts = new HashMap<>();
    results.allEvents().reportingEntryPublished().stream()
        .map(event -> event.getRequiredPayload(ReportEntry.class))
        .forEach(entry -> facts.putAll(entry.getKeyValuePairs()));
    return facts;
  }

  private static Throwable failure(EngineExecutionResults results) {
    return results.testEvents().failed().stream()
        .findFirst()
        .orElseThrow()
        .getRequiredPayload(TestExecutionResult.class)
        .getThrowable()
        .orElseThrow();
  }

  private static List<String> list(String value) {
    return value.isEmpty() ? List.of() : Arrays.asList(value.split(","));
  }

  private static void assertUserDeleted(String username) {
    assertNotNull(username, "probe did not report the username");
    assertFalse(new Profile(username).exists(), "API profile left for " + username);
    assertFalse(AuthAccount.of(username, "").exists(), "auth account left for " + username);
  }

  private static void report(TestReporter reporter, String prefix, TestUser user) {
    reporter.publishEntry(Map.of(
        prefix + ".username", user.username(),
        prefix + ".authExists", String.valueOf(AuthAccount.of(user.username(), "").exists()),
        prefix + ".profileExists", String.valueOf(new Profile(user.username()).exists())
    ));
  }

  @Tag(PROBE)
  static class SingleUserProbe {

    @Test
    void probe(@User TestUser user, TestReporter reporter) {
      report(reporter, "user", user);
    }
  }

  @Tag(PROBE)
  static class FailingTestProbe {

    @Test
    void probe(@User TestUser user, TestReporter reporter) {
      report(reporter, "user", user);
      fail("probe fails on purpose");
    }
  }

  @Tag(PROBE)
  static class PreconditionsProbe {

    @Test
    void probe(@User(friends = 2, incomeInvitations = 1) TestUser user, TestReporter reporter) {
      report(reporter, "user", user);
      boolean allExist = user.friends().stream()
          .allMatch(friend -> Friendship.friends(friend.username(), user.username()).exists())
          && user.incomeInvitations().stream()
          .allMatch(inviter -> Friendship.invitation(inviter.username(), user.username()).exists());
      reporter.publishEntry(Map.of(
          "friends", user.friends().stream().map(TestUser::username).collect(Collectors.joining(",")),
          "inviters", user.incomeInvitations().stream().map(TestUser::username).collect(Collectors.joining(",")),
          "friendships.exist", String.valueOf(allExist)
      ));
    }
  }

  @Tag(PROBE)
  static class TwoUsersProbe {

    @Test
    void probe(@User TestUser first, @User TestUser second, TestReporter reporter) {
      report(reporter, "first", first);
      report(reporter, "second", second);
    }
  }

  @Tag(PROBE)
  static class CreationFailureProbe {

    @Test
    void probe(@User(username = CONFLICT_USERNAME) TestUser user) {
    }
  }

  @Tag(PROBE)
  static class UnsupportedTypeProbe {

    @Test
    void probe(@User String user) {
    }
  }
}
