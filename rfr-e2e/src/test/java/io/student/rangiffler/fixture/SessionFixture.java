package io.student.rangiffler.fixture;

public interface SessionFixture<S> extends AutoCloseable {

  S create();

  @Override
  void close();
}
