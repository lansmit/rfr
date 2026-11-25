package io.student.rangiffler.fixture;

import com.microsoft.playwright.BrowserContext;

public interface SessionFixture<S> extends AutoCloseable {

  S create();

  BrowserContext context();

  @Override
  void close();
}
