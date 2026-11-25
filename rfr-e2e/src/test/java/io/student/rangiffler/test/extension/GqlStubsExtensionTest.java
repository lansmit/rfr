package io.student.rangiffler.test.extension;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.extension.ProbeTargetCondition;
import io.student.rangiffler.network.GqlErrorType;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.session.GuestSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@DisplayName("GqlStubsExtension")
class GqlStubsExtensionTest {

  @Test
  @DisplayName("A test with two GqlStubs parameters fails with ParameterResolutionException pointing to @Session(useStubs = true)")
  void twoStubsParametersShouldFail() {
    Throwable failure = failure(run(GqlStubsTwoParametersProbe.class));

    assertInstanceOf(ParameterResolutionException.class, failure);
    assertNull(failure.getCause(), "JUnit wrapped another exception into ParameterResolutionException: " + failure.getCause());
    assertTrue(failure.getMessage().contains("@Session(useStubs = true)"),
        "message does not point to @Session(useStubs = true): " + failure.getMessage());
  }

  @Test
  @DisplayName("Session stubs are empty without useStubs and present with useStubs = true")
  void sessionStubsShouldFollowUseStubs() {
    EngineExecutionResults results = run(GqlStubsSessionOptionalProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
    Map<String, String> facts = facts(results);
    assertEquals("false", facts.get("withoutUseStubs"));
    assertEquals("true", facts.get("withUseStubs"));
  }

  @Test
  @DisplayName("A test with stubs in the main context and in a session closes both without errors")
  void stubsOfBothContextsShouldCloseWithoutErrors() {
    EngineExecutionResults results = run(GqlStubsCloseProbe.class);

    results.testEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class))
        .flatMap(result -> result.getThrowable().stream())
        .findFirst()
        .ifPresent(failure -> fail("closing the stubs failed: " + failure, failure));
    results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
  }

  // Playwright keeps its browser per thread: a thread of its own keeps the probe's browser apart from the caller's
  private static EngineExecutionResults run(Class<?> probe) {
    ExecutorService thread = Executors.newSingleThreadExecutor();
    try {
      return thread.submit(() -> EngineTestKit.engine("junit-jupiter")
          .configurationParameter(ProbeTargetCondition.RUN_PROBES, "true")
          .selectors(selectClass(probe))
          .execute()).get(3, TimeUnit.MINUTES);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted while " + probe.getSimpleName() + " ran", exception);
    } catch (ExecutionException | TimeoutException exception) {
      throw new IllegalStateException(probe.getSimpleName() + " did not finish", exception);
    } finally {
      thread.shutdownNow();
    }
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
        .map(event -> event.getRequiredPayload(TestExecutionResult.class))
        .flatMap(result -> result.getThrowable().stream())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("probe did not fail"));
  }
}

@UiTest
@ProbeTarget
final class GqlStubsTwoParametersProbe {

  @Test
  void probe(GqlStubs first, GqlStubs second) {
  }
}

@UiTest
@ProbeTarget
final class GqlStubsSessionOptionalProbe {

  @Test
  void probe(@Session GuestSession withoutUseStubs,
             @Session(useStubs = true) GuestSession withUseStubs,
             TestReporter reporter) {
    reporter.publishEntry(Map.of(
        "withoutUseStubs", String.valueOf(withoutUseStubs.stubs().isPresent()),
        "withUseStubs", String.valueOf(withUseStubs.stubs().isPresent())));
  }
}

@UiTest
@ProbeTarget
final class GqlStubsCloseProbe {

  @Test
  void probe(Page page, GqlStubs stubs, @Session(useStubs = true) GuestSession guest) {
    stubs.error("GetUser", GqlErrorType.INTERNAL_ERROR, "main context stub");
    guest.stubs().orElseThrow().error("GetUser", GqlErrorType.INTERNAL_ERROR, "session stub");
  }
}
