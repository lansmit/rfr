package io.student.rangiffler.test.extension;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.SessionExtension;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

// Probe targets are UI tests: they need the stand and a browser.
// They are top-level classes, not nested ones: Playwright looks for @UsePlaywright (@UiTest) on the outermost class only.
@DisplayName("SessionExtension")
class SessionExtensionTest {

  static final String GUEST_WITH_USER_USERNAME = "rfr_probe_guest_with_user";

  @Test
  @DisplayName("Guest session test succeeds, opens a second context beside Playwright's one and closes it after the test")
  void guestSessionShouldOpenAndCloseSecondContext() {
    EngineExecutionResults results = run(SessionGuestProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
    Map<String, String> facts = facts(results);
    assertEquals("2", facts.get("contextsDuring"));
    assertEquals("1", facts.get("contextsAfter"));
  }

  @Test
  @DisplayName("User session reaches the main page, its user exists during the test, user and context are gone after it")
  void userSessionShouldLogInAndLeaveNothing() {
    EngineExecutionResults results = run(SessionUserProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(2).succeeded(2));
    Map<String, String> facts = facts(results);
    assertEquals("true", facts.get("accountDuring"));
    assertAccountDeleted(facts.get("username"));
    assertEquals("1", facts.get("contextsAfter"));
  }

  @Test
  @DisplayName("Session user and @User TestUser of the same test are different users")
  void sessionAndMainUserShouldDiffer() {
    Map<String, String> facts = facts(run(SessionBesideMainUserProbe.class));

    assertNotEquals(facts.get("main"), facts.get("session"));
  }

  @Test
  @DisplayName("After a failed test the session user and context are gone")
  void failedTestShouldLeaveNothing() {
    EngineExecutionResults results = run(SessionFailingProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(2).failed(1).succeeded(1));
    Map<String, String> facts = facts(results);
    assertAccountDeleted(facts.get("username"));
    assertEquals("1", facts.get("contextsAfter"));
  }

  @Test
  @DisplayName("UserSession without @User fails with ExtensionConfigurationException and opens no context")
  void userSessionWithoutUserShouldFail() {
    EngineExecutionResults results = run(SessionUserWithoutUserProbe.class);

    assertInstanceOf(ExtensionConfigurationException.class, failure(results));
    assertEquals("1", facts(results).get("contextsAfter"));
  }

  @Test
  @DisplayName("GuestSession with @User fails with ExtensionConfigurationException, opens no context, and its user is deleted")
  void guestSessionWithUserShouldFail() {
    EngineExecutionResults results = run(SessionGuestWithUserProbe.class);

    assertInstanceOf(ExtensionConfigurationException.class, failure(results));
    assertEquals("1", facts(results).get("contextsAfter"));
    assertAccountDeleted(GUEST_WITH_USER_USERNAME);
  }

  @Test
  @DisplayName("SessionExtension over a class opens no context for a test without @Session")
  void testWithoutSessionShouldGetNoContext() {
    EngineExecutionResults results = run(SessionAbsentProbe.class);

    results.testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
    assertEquals("1", facts(results).get("contextsDuring"));
  }

  @Test
  @DisplayName("A guest browser stays logged out while another browser in the same test is logged in")
  void guestBrowserShouldNotShareLogin() {
    run(SessionIsolationProbe.class).testEvents().assertStatistics(stats -> stats.started(1).succeeded(1));
  }

  // Playwright keeps its browser per thread: a thread of its own keeps the probe's browser apart from the caller's
  private static EngineExecutionResults run(Class<?> probe) {
    ExecutorService thread = Executors.newSingleThreadExecutor();
    try {
      return thread.submit(() -> EngineTestKit.engine("junit-jupiter")
          .configurationParameter(ProbeTargetCondition.RUN_PROBES, "true")
          .selectors(selectClass(probe))
          .execute()).get(3, TimeUnit.MINUTES);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("interrupted while " + probe.getSimpleName() + " ran", e);
    } catch (ExecutionException | TimeoutException e) {
      throw new IllegalStateException(probe.getSimpleName() + " did not finish", e);
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

  private static void assertAccountDeleted(String username) {
    assertFalse(username == null, "probe did not report the username");
    assertFalse(AuthAccount.of(username, "").exists(), "auth account left for " + username);
  }

  static String contexts(Page page) {
    return String.valueOf(page.context().browser().contexts().size());
  }
}

@UiTest
@ProbeTarget
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class SessionGuestProbe {

  @Test
  @Order(1)
  void probe(@Session GuestSession guest, Page page, TestReporter reporter) {
    reporter.publishEntry("contextsDuring", SessionExtensionTest.contexts(page));
    guest.landingPage().checkUserIsNotLoggedIn();
  }

  @Test
  @Order(2)
  void afterwards(Page page, TestReporter reporter) {
    reporter.publishEntry("contextsAfter", SessionExtensionTest.contexts(page));
  }
}

@UiTest
@ProbeTarget
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class SessionUserProbe {

  @Test
  @Order(1)
  void probe(@Session @User UserSession bob, TestReporter reporter) {
    reporter.publishEntry(Map.of(
        "username", bob.user().username(),
        "accountDuring", String.valueOf(AuthAccount.of(bob.user().username(), "").exists())));
    bob.mainPage().header().checkUserIsLoggedIn();
  }

  @Test
  @Order(2)
  void afterwards(Page page, TestReporter reporter) {
    reporter.publishEntry("contextsAfter", SessionExtensionTest.contexts(page));
  }
}

@UiTest
@ProbeTarget
final class SessionBesideMainUserProbe {

  @Test
  void probe(@User TestUser alice, @Session @User UserSession bob, TestReporter reporter) {
    reporter.publishEntry(Map.of("main", alice.username(), "session", bob.user().username()));
  }
}

@UiTest
@ProbeTarget
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class SessionFailingProbe {

  @Test
  @Order(1)
  void probe(@Session @User UserSession bob, TestReporter reporter) {
    reporter.publishEntry("username", bob.user().username());
    throw new IllegalStateException("probe fails on purpose");
  }

  @Test
  @Order(2)
  void afterwards(Page page, TestReporter reporter) {
    reporter.publishEntry("contextsAfter", SessionExtensionTest.contexts(page));
  }
}

@UiTest
@ProbeTarget
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class SessionUserWithoutUserProbe {

  @Test
  @Order(1)
  void probe(@Session UserSession bob) {
  }

  @Test
  @Order(2)
  void afterwards(Page page, TestReporter reporter) {
    reporter.publishEntry("contextsAfter", SessionExtensionTest.contexts(page));
  }
}

@UiTest
@ProbeTarget
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
final class SessionGuestWithUserProbe {

  @Test
  @Order(1)
  void probe(@Session @User(username = SessionExtensionTest.GUEST_WITH_USER_USERNAME) GuestSession guest) {
  }

  @Test
  @Order(2)
  void afterwards(Page page, TestReporter reporter) {
    reporter.publishEntry("contextsAfter", SessionExtensionTest.contexts(page));
  }
}

@UiTest
@ProbeTarget
@ExtendWith(SessionExtension.class)
final class SessionAbsentProbe {

  @Test
  void probe(Page page, TestReporter reporter) {
    reporter.publishEntry("contextsDuring", SessionExtensionTest.contexts(page));
  }
}

@UiTest
@ProbeTarget
final class SessionIsolationProbe {

  @Test
  void probe(@Session @User UserSession user, @Session GuestSession guest) {
    user.mainPage().header().checkUserIsLoggedIn();
    guest.landingPage().checkUserIsNotLoggedIn();
  }
}
