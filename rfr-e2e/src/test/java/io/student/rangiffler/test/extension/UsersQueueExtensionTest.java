package io.student.rangiffler.test.extension;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import io.student.rangiffler.jupiter.extension.ProbeTargetCondition;
import io.student.rangiffler.model.TestUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@DisplayName("UsersQueueExtension")
class UsersQueueExtensionTest {


  @Test
  @DisplayName("Four tests in a row pass on a pool of three users")
  void fourTestsShouldPassOnPoolOfThree() {
    EngineExecutionResults results = run(FourTestsProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(4).succeeded(4));
  }

  @Test
  @DisplayName("User taken by a failed test is returned to the queue")
  void userShouldBeReturnedAfterFailedTest() {
    EngineExecutionResults failed = run(FailingTestProbe.class);
    failed.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
    String username = facts(failed).get("user.username");
    assertNotNull(username, "probe did not report the username");

    EngineExecutionResults wholePool = run(WholePoolProbe.class);
    wholePool.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
    Map<String, String> facts = facts(wholePool);
    List<String> pool = List.of(facts.get("first.username"), facts.get("second.username"), facts.get("third.username"));
    assertTrue(pool.contains(username), username + " is not back in the queue, pool: " + pool);
  }

  @Test
  @DisplayName("Fourth user from a pool of three fails with a clear error after the timeout")
  void emptyPoolShouldFailWithClearErrorAfterTimeout() {
    EngineExecutionResults results = run(FourUsersInOneTestProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(1).failed(1));
    Throwable failure = failure(results);
    assertInstanceOf(ParameterResolutionException.class, failure);
    assertInstanceOf(IllegalStateException.class, failure.getCause());
    assertTrue(failure.getCause().getMessage().startsWith("No free static user"), failure.getCause().getMessage());
  }

  @AfterEach
  void allPoolUsersShouldBeBackInQueue() {
    run(WholePoolProbe.class).testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
  }

  private static EngineExecutionResults run(Class<?> probe) {
    return EngineTestKit.engine("junit-jupiter")
        .configurationParameter(ProbeTargetCondition.RUN_PROBES, "true")
        .selectors(selectClass(probe))
        .execute();
  }

  private static Throwable failure(EngineExecutionResults results) {
    return results.testEvents().failed().stream()
        .findFirst()
        .orElseThrow()
        .getRequiredPayload(TestExecutionResult.class)
        .getThrowable()
        .orElseThrow();
  }

  private static Map<String, String> facts(EngineExecutionResults results) {
    Map<String, String> facts = new HashMap<>();
    results.allEvents().reportingEntryPublished().stream()
        .map(event -> event.getRequiredPayload(ReportEntry.class))
        .forEach(entry -> facts.putAll(entry.getKeyValuePairs()));
    return facts;
  }

  @ProbeTarget
  static class FourTestsProbe {

    @Test
    void first(@StaticUser TestUser user) {
    }

    @Test
    void second(@StaticUser TestUser user) {
    }

    @Test
    void third(@StaticUser TestUser user) {
    }

    @Test
    void fourth(@StaticUser TestUser user) {
    }
  }

  @ProbeTarget
  static class FourUsersInOneTestProbe {

    @Test
    void probe(@StaticUser TestUser first, @StaticUser TestUser second, @StaticUser TestUser third,
               @StaticUser TestUser fourth) {
    }
  }

  @ProbeTarget
  static class FailingTestProbe {

    @Test
    void probe(@StaticUser TestUser user, TestReporter reporter) {
      reporter.publishEntry("user.username", user.username());
      fail("probe fails on purpose");
    }
  }

  @ProbeTarget
  static class WholePoolProbe {

    @Test
    void probe(@StaticUser TestUser first, @StaticUser TestUser second, @StaticUser TestUser third,
               TestReporter reporter) {
      reporter.publishEntry(Map.of(
          "first.username", first.username(),
          "second.username", second.username(),
          "third.username", third.username()
      ));
    }
  }
}
