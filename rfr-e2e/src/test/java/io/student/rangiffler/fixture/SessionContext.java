package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.config.BrowserSettings;
import io.student.rangiffler.network.GqlStubs;

import java.util.Optional;

final class SessionContext {

  private final Browser browser;
  private final boolean useStubs;
  private BrowserContext context;
  private GqlStubs stubs;

  SessionContext(Browser browser, boolean useStubs) {
    this.browser = browser;
    this.useStubs = useStubs;
  }

  Page open() {
    context = BrowserSettings.newContext(browser);
    BrowserSettings.startTracing(context, "session");
    if (useStubs) {
      stubs = new GqlStubs(context);
    }
    return context.newPage();
  }

  BrowserContext context() {
    return context;
  }

  Optional<GqlStubs> stubs() {
    return Optional.ofNullable(stubs);
  }

  void close() {
    Cleanup.runAll(
        () -> {
          if (stubs != null) {
            stubs.close();
          }
        },
        () -> {
          if (context != null) {
            context.close();
          }
        }
    );
  }
}
