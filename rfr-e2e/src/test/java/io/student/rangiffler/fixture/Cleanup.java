package io.student.rangiffler.fixture;

import java.util.List;

final class Cleanup {

  private Cleanup() {
  }

  static void runAll(Runnable... cleanups) {
    runAll(List.of(cleanups));
  }

  // each cleanup is independent: a failure in one must not skip the rest or hide their errors
  static void runAll(List<Runnable> cleanups) {
    RuntimeException failure = null;
    for (Runnable cleanup : cleanups) {
      try {
        cleanup.run();
      } catch (RuntimeException e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }
}
