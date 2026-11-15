package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import io.student.rangiffler.config.BrowserSettings;

import java.nio.file.Path;

final class SessionContext {

  private final Browser browser;
  private BrowserContext context;

  SessionContext(Browser browser) {
    this.browser = browser;
  }

  Page open() {
    context = BrowserSettings.newContext(browser);
    context.tracing().start(BrowserSettings.traceOptions());
    return context.newPage();
  }

  void saveTrace(Path trace) {
    if (context != null) {
      context.tracing().stop(new Tracing.StopOptions().setPath(trace));
    }
  }

  void discardTrace() {
    if (context != null) {
      context.tracing().stop();
    }
  }

  void close() {
    if (context != null) {
      context.close();
    }
  }
}
