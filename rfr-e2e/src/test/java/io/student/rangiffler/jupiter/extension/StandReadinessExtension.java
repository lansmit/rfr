package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.stand.StandService;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class StandReadinessExtension implements SuiteExtension {

  private static final String UI_TAG = "ui";
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration MIN_REQUEST_TIMEOUT = Duration.ofMillis(100);

  private final boolean enabled;
  private final Duration timeout;
  private final List<StandService> services;

  public StandReadinessExtension() {
    this(TestConfig.standCheck(), TestConfig.standTimeout(), StandService.fromConfig());
  }

  protected StandReadinessExtension(boolean enabled, Duration timeout, List<StandService> services) {
    this.enabled = enabled;
    this.timeout = timeout;
    this.services = List.copyOf(services);
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (enabled && context.getTags().contains(UI_TAG)) {
      SuiteExtension.super.beforeAll(context);
    }
  }

  @Override
  public void beforeSuite(ExtensionContext context) {
    Instant deadline = Instant.now().plus(timeout);
    List<StandService> pending = new ArrayList<>(services);
    while (true) {
      pending.removeIf(service -> service.check().responds(requestTimeout(deadline)));
      if (pending.isEmpty()) {
        return;
      }
      Duration remaining = Duration.between(Instant.now(), deadline);
      if (remaining.isNegative() || remaining.isZero()) {
        throw new IllegalStateException(notReady(pending));
      }
      sleep(POLL_INTERVAL.compareTo(remaining) < 0 ? POLL_INTERVAL : remaining);
    }
  }

  private static Duration requestTimeout(Instant deadline) {
    Duration remaining = Duration.between(Instant.now(), deadline);
    if (remaining.compareTo(MIN_REQUEST_TIMEOUT) < 0) {
      return MIN_REQUEST_TIMEOUT;
    }
    return remaining.compareTo(REQUEST_TIMEOUT) < 0 ? remaining : REQUEST_TIMEOUT;
  }

  private String notReady(List<StandService> pending) {
    return "Stand is not ready after " + timeout.toSeconds() + " s, these services did not respond: "
        + pending.stream()
            .map(service -> service.name() + " (start it with '" + service.startCommand() + "')")
            .collect(Collectors.joining(", "))
        + ". Wait longer with -Drfr.standTimeout=<seconds> or skip the wait with -Drfr.standCheck=false";
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting for the stand", exception);
    }
  }
}
