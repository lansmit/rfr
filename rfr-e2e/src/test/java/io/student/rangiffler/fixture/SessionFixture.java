package io.student.rangiffler.fixture;

import java.nio.file.Path;

public interface SessionFixture<S> extends AutoCloseable {

  S create();

  void saveTrace(Path trace);

  void discardTrace();

  @Override
  void close();
}
