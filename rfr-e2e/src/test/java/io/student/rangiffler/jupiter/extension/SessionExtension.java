package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.impl.junit.BrowserExtension;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.fixture.GuestSessionFixture;
import io.student.rangiffler.fixture.SessionFixture;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.session.GuestSession;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

// The browser is only reachable through Playwright's internal impl class: may break on upgrade.
public class SessionExtension implements ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SessionExtension.class);

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(Session.class)
        && parameterContext.getParameter().getType() == GuestSession.class
        && extensionContext.getTestMethod().filter(parameterContext.getDeclaringExecutable()::equals).isPresent();
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    if (!AnnotationSupport.isAnnotated(extensionContext.getRequiredTestClass(), UsePlaywright.class)) {
      throw new ParameterResolutionException("@Session needs a browser: annotate the test class with @UiTest");
    }
    if (parameterContext.isAnnotated(User.class)) {
      throw new ParameterResolutionException("GuestSession has no user: remove @User or take a UserSession");
    }
    Browser browser = BrowserExtension.getOrCreateBrowser(extensionContext);
    SessionFixture<?> fixture = new GuestSessionFixture(browser);
    extensionContext.getStore(NAMESPACE).put(parameterContext.getIndex(), fixture);
    return fixture.create();
  }
}
