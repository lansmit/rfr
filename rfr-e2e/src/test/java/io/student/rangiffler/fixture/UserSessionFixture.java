package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.config.BrowserSettings;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.session.UserSession;

import java.util.ArrayList;
import java.util.List;

public final class UserSessionFixture implements SessionFixture<UserSession> {

  private final Browser browser;
  private final UserFixture delegate;
  private BrowserContext context;

  public UserSessionFixture(Browser browser, UserFixture delegate) {
    this.browser = browser;
    this.delegate = delegate;
  }

  @Override
  public UserSession create() {
    TestUser user = delegate.create();
    context = BrowserSettings.newContext(browser);
    Page page = context.newPage();
    // waits for the redirect after login, otherwise mainPage() would hand out a page that is still logging in
    LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .checkIsOpened()
        .header()
        .checkUserIsLoggedIn();
    return new UserSession(user, page);
  }

  @Override
  public void close() {
    List<Runnable> cleanups = new ArrayList<>();
    if (context != null) {
      cleanups.add(context::close);
    }
    cleanups.add(delegate::close);
    Cleanup.runAll(cleanups);
  }
}
