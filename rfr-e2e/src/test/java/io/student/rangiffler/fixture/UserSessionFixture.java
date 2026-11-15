package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.session.UserSession;

import java.nio.file.Path;

// The user belongs to UserExtension: this fixture owns only the browser context and never deletes the user.
public final class UserSessionFixture implements SessionFixture<UserSession> {

  private final SessionContext context;
  private final TestUser user;

  public UserSessionFixture(Browser browser, TestUser user) {
    this.context = new SessionContext(browser);
    this.user = user;
  }

  @Override
  public UserSession create() {
    Page page = context.open();
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
