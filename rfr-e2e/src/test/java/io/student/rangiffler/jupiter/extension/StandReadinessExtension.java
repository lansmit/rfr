package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.stand.StandService;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StandReadinessExtension implements SuiteExtension {

  private static final String UI_TAG = "ui";
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(2);
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration MIN_REQUEST_TIMEOUT = Duration.ofMillis(100);

  private final Supplier<Boolean> enabled;
  private final Supplier<Duration> timeout;
  private final Supplier<List<StandService>> services;

  public StandReadinessExtension() {
    this(TestConfig::standCheck, TestConfig::standTimeout, StandService::fromConfig);
  }

  protected StandReadinessExtension(
      Supplier<Boolean> enabled,
      Supplier<Duration> timeout,
      Supplier<List<StandService>> services
  ) {
    this.enabled = enabled;
    this.timeout = timeout;
    this.services = services;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (context.getTags().contains(UI_TAG) && enabled.get()) {
      SuiteExtension.super.beforeAll(context);
    }
  }

  @Override
  public void beforeSuite(ExtensionContext context) {
    Duration timeout = this.timeout.get();
    long deadline = System.nanoTime() + timeout.toNanos();
    List<StandService> pending = new ArrayList<>(services.get());
    while (true) {
      pending.removeIf(service -> beforeDeadline(deadline) && service.check().responds(requestTimeout(deadline)));
      if (pending.isEmpty()) {
        return;
      }
      Duration remaining = Duration.ofNanos(deadline - System.nanoTime());
      if (remaining.isNegative() || remaining.isZero()) {
        throw new IllegalStateException(notReady(pending, timeout));
      }
      sleep(POLL_INTERVAL.compareTo(remaining) < 0 ? POLL_INTERVAL : remaining);
    }
  }

  private static boolean beforeDeadline(long deadline) {
    return System.nanoTime() - deadline < 0;
  }

  private static Duration requestTimeout(long deadline) {
    Duration remaining = Duration.ofNanos(deadline - System.nanoTime());
    if (remaining.compareTo(MIN_REQUEST_TIMEOUT) < 0) {
      return MIN_REQUEST_TIMEOUT;
    }
    return remaining.compareTo(REQUEST_TIMEOUT) < 0 ? remaining : REQUEST_TIMEOUT;
  }

  private static String notReady(List<StandService> pending, Duration timeout) {
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
