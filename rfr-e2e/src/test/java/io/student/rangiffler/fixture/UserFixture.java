package io.student.rangiffler.fixture;

import io.student.rangiffler.model.TestUser;

public interface UserFixture extends AutoCloseable {

  TestUser create();

  @Override
  void close();
}
