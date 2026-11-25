package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.config.BrowserSettings;
import io.student.rangiffler.network.GqlStubs;

import java.util.Optional;
import java.util.function.Function;

final class SessionContext {

  private final Browser browser;
  private final Function<BrowserContext, Optional<GqlStubs>> stubsFactory;
  private BrowserContext context;
  private GqlStubs stubs;

  SessionContext(Browser browser, Function<BrowserContext, Optional<GqlStubs>> stubsFactory) {
    this.browser = browser;
    this.stubsFactory = stubsFactory;
  }

  Page open() {
    context = BrowserSettings.newContext(browser);
    BrowserSettings.startTracing(context, "session");
    stubs = stubsFactory.apply(context).orElse(null);
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
