package io.student.rangiffler.fixture;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.session.UserSession;

import java.nio.file.Path;

public final class UserSessionFixture implements SessionFixture<UserSession> {

  private final SessionContext context;
  private final UserFixture delegate;

  public UserSessionFixture(Browser browser, UserFixture delegate) {
    this.context = new SessionContext(browser);
    this.delegate = delegate;
  }

  @Override
  public UserSession create() {
    TestUser user = delegate.create();
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
    Cleanup.runAll(context::close, delegate::close);
  }
}
