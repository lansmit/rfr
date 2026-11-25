package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.session.GuestSession;

import java.util.Optional;
import java.util.function.Function;

public final class GuestSessionFixture implements SessionFixture<GuestSession> {

  private final SessionContext context;

  public GuestSessionFixture(Browser browser, Function<BrowserContext, Optional<GqlStubs>> stubsFactory) {
    this.context = new SessionContext(browser, stubsFactory);
  }

  @Override
  public GuestSession create() {
    Page page = context.open();
    LandingPage.open(page);
    return new GuestSession(page, context.stubs());
  }

  @Override
  public BrowserContext context() {
    return context.context();
  }

  @Override
  public void close() {
    context.close();
  }
}
