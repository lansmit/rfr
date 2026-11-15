package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.config.BrowserSettings;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.session.GuestSession;

public final class GuestSessionFixture implements SessionFixture<GuestSession> {

  private final Browser browser;
  private BrowserContext context;

  public GuestSessionFixture(Browser browser) {
    this.browser = browser;
  }

  @Override
  public GuestSession create() {
    context = BrowserSettings.newContext(browser);
    Page page = context.newPage();
    LandingPage.open(page);
    return new GuestSession(page);
  }

  @Override
  public void close() {
    if (context != null) {
      context.close();
    }
  }
}
