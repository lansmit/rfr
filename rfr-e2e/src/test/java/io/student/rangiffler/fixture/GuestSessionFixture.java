package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.session.GuestSession;

import java.nio.file.Path;

public final class GuestSessionFixture implements SessionFixture<GuestSession> {

  private final SessionContext context;

  public GuestSessionFixture(Browser browser) {
    this.context = new SessionContext(browser);
  }

  @Override
  public GuestSession create() {
    Page page = context.open();
    LandingPage.open(page);
    return new GuestSession(page);
  }

  @Override
  public void saveTrace(Path trace) {
    context.saveTrace(trace);
  }

  @Override
  public void discardTrace() {
    context.discardTrace();
  }

  @Override
  public void close() {
    context.close();
  }
}
