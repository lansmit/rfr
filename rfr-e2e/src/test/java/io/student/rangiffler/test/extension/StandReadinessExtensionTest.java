package io.student.rangiffler.test.extension;

import com.sun.net.httpserver.HttpServer;
import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.StandReadinessExtension;
import io.student.rangiffler.stand.StandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@DisplayName("StandReadinessExtension")
class StandReadinessExtensionTest {

  private static final String SERVICE = "fake-service";
  private static final String BROKEN_CONFIG = "rfr.standTimeout must be a whole number of seconds, got 'abc'";

  private static volatile URI serviceUri;

  @Test
  @DisplayName("Сервис начал отвечать через 2 с — тесты выполняются")
  void testsRunWhenServiceRespondsLater() {
    try (FakeService service = new FakeService(Duration.ofSeconds(2))) {
      EngineExecutionResults results = run(PatientTarget.class);

      assertEquals(1L, results.testEvents().succeeded().count(), "The test did not run after the service came up");
      assertTrue(service.requests() > 1, "The service was not polled again after the first failed request");
    }
  }

  @Test
  @DisplayName("Сервис не ответил за таймаут — тесты не выполняются, а в ошибке есть имя сервиса")
  void testsDoNotRunWhenServiceNeverResponds() {
    try (FakeService ignored = new FakeService(Duration.ofDays(1))) {
      EngineExecutionResults results = run(ImpatientTarget.class);

      assertEquals(0L, results.testEvents().started().count(), "A test ran although the service did not respond");
      assertTrue(failureMessages(results).stream().anyMatch(message -> message.contains(SERVICE)),
          "The failure does not name the service that did not respond: " + failureMessages(results));
    }
  }

  @Test
  @DisplayName("Класс без тега UI-тестов — к сервису нет ни одного запроса")
  void classWithoutUiTagSendsNoRequests() {
    try (FakeService service = new FakeService(Duration.ZERO)) {
      EngineExecutionResults results = run(UntaggedTarget.class);

      assertEquals(1L, results.testEvents().succeeded().count(), "The test without the ui tag did not run");
      assertEquals(0, service.requests(), "A class without the ui tag polled the stand");
    }
  }

  @Test
  @DisplayName("Проверка выключена — к сервису нет ни одного запроса")
  void disabledCheckSendsNoRequests() {
    try (FakeService service = new FakeService(Duration.ZERO)) {
      EngineExecutionResults results = run(DisabledCheckTarget.class);

      assertEquals(1L, results.testEvents().succeeded().count(), "The test did not run with the check disabled");
      assertEquals(0, service.requests(), "The disabled check polled the stand");
    }
  }

  @Test
  @DisplayName("Неверный конфиг ожидания не мешает классу без тега UI-тестов")
  void brokenConfigDoesNotBreakClassWithoutUiTag() {
    try (FakeService service = new FakeService(Duration.ZERO)) {
      EngineExecutionResults results = run(BrokenConfigUntaggedTarget.class);

      assertEquals(1L, results.testEvents().succeeded().count(), "Broken stand config broke a test without the ui tag");
      assertEquals(0, service.requests(), "A class without the ui tag polled the stand");
    }
  }

  @Test
  @DisplayName("Неверный конфиг ожидания валит UI-класс ошибкой с именем ключа")
  void brokenConfigFailsUiClassWithKeyName() {
    try (FakeService ignored = new FakeService(Duration.ZERO)) {
      EngineExecutionResults results = run(BrokenConfigUiTarget.class);

      assertEquals(0L, results.testEvents().started().count(), "A ui test ran with a broken stand config");
      assertTrue(failureMessages(results).stream().anyMatch(message -> message.contains("rfr.standTimeout")),
          "The failure does not name the broken key: " + failureMessages(results));
    }
  }

  private static EngineExecutionResults run(Class<?> target) {
    return EngineTestKit.engine("junit-jupiter")
        .configurationParameter(ProbeTargetCondition.RUN_PROBES, "true")
        .selectors(selectClass(target))
        .execute();
  }

  private static List<String> failureMessages(EngineExecutionResults results) {
    return results.containerEvents().failed().stream()
        .map(event -> event.getRequiredPayload(TestExecutionResult.class))
        .flatMap(result -> result.getThrowable().stream())
        .map(Throwable::getMessage)
        .toList();
  }

  private static StandService fakeService() {
    return StandService.httpGet(SERVICE, "start " + SERVICE, serviceUri);
  }

  static final class FakeService implements AutoCloseable {

    private final HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();

    FakeService(Duration readyAfter) {
      Instant readyAt = Instant.now().plus(readyAfter);
      try {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      } catch (IOException exception) {
        throw new UncheckedIOException("Cannot start the fake service", exception);
      }
      server.createContext("/", exchange -> {
        requests.incrementAndGet();
        exchange.sendResponseHeaders(Instant.now().isBefore(readyAt) ? 503 : 200, -1);
        exchange.close();
      });
      server.start();
      serviceUri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    int requests() {
      return requests.get();
    }

    @Override
    public void close() {
      server.stop(0);
    }
  }

  static final class PatientStand extends StandReadinessExtension {
    PatientStand() {
      super(() -> true, () -> Duration.ofSeconds(10), () -> List.of(fakeService()));
    }
  }

  static final class ImpatientStand extends StandReadinessExtension {
    ImpatientStand() {
      super(() -> true, () -> Duration.ofSeconds(1), () -> List.of(fakeService()));
    }
  }

  static final class DisabledStand extends StandReadinessExtension {
    DisabledStand() {
      super(() -> false, () -> Duration.ofSeconds(10), () -> List.of(fakeService()));
    }
  }

  static final class BrokenConfigStand extends StandReadinessExtension {
    BrokenConfigStand() {
      super(() -> true, () -> {
        throw new IllegalArgumentException(BROKEN_CONFIG);
      }, () -> List.of(fakeService()));
    }
  }

  @ProbeTarget
  @Tag("ui")
  @ExtendWith(PatientStand.class)
  static final class PatientTarget {
    @Test
    void runs() {
    }
  }

  @ProbeTarget
  @Tag("ui")
  @ExtendWith(ImpatientStand.class)
  static final class ImpatientTarget {
    @Test
    void runs() {
    }
  }

  @ProbeTarget
  @ExtendWith(PatientStand.class)
  static final class UntaggedTarget {
    @Test
    void runs() {
    }
  }

  @ProbeTarget
  @ExtendWith(BrokenConfigStand.class)
  static final class BrokenConfigUntaggedTarget {
    @Test
    void runs() {
    }
  }

  @ProbeTarget
  @Tag("ui")
  @ExtendWith(BrokenConfigStand.class)
  static final class BrokenConfigUiTarget {
    @Test
    void runs() {
    }
  }

  @ProbeTarget
  @Tag("ui")
  @ExtendWith(DisabledStand.class)
  static final class DisabledCheckTarget {
    @Test
    void runs() {
    }
  }
}
