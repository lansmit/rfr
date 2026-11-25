package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.config.BrowserSettings;

final class SessionContext {

  private final Browser browser;
  private BrowserContext context;

  SessionContext(Browser browser) {
    this.browser = browser;
  }

  Page open() {
    context = BrowserSettings.newContext(browser);
    BrowserSettings.startTracing(context, "session");
    return context.newPage();
  }

  BrowserContext context() {
    return context;
  }

  void close() {
    if (context != null) {
      context.close();
    }
  }
}
