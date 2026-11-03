package io.student.rangiffler.jupiter.extension;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.ExtensionContext;

final class SuiteRun implements AutoCloseable {
  private final SuiteExtension extension;
  private final ExtensionContext context;
  private final AtomicBoolean started;
  private final List<Throwable> failures;

  SuiteRun(SuiteExtension extension, ExtensionContext context) {
    this(extension, context, new AtomicBoolean(), new CopyOnWriteArrayList<>());
  }

  private SuiteRun(
      SuiteExtension extension,
      ExtensionContext context,
      AtomicBoolean started,
      List<Throwable> failures
  ) {
    this.extension = extension;
    this.context = context;
    this.started = started;
    this.failures = failures;
  }

  synchronized void start() {
    if (started.compareAndSet(false, true)) {
      try {
        extension.beforeSuite(context);
      } catch (RuntimeException | Error failure) {
        failures.add(failure);
      }
    }
    if (!failures.isEmpty()) {
      Throwable failure = failures.getFirst();
      if (failure instanceof Error error) {
        throw error;
      }
      throw (RuntimeException) failure;
    }
  }

  @Override
  public void close() {
    try {
      extension.afterSuite(context);
    } catch (RuntimeException | Error failure) {
      failures.forEach(failure::addSuppressed);
      throw failure;
    }
  }
}
