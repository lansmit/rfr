package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.impl.junit.BrowserExtension;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.fixture.GuestSessionFixture;
import io.student.rangiffler.fixture.SessionFixture;
import io.student.rangiffler.fixture.UserSessionFixture;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;
import java.io.IOException;
import java.lang.reflect.Parameter;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

// Sessions are created in beforeTestExecution, not beforeEach: the user of a UserSession comes from UserExtension,
// and only by beforeTestExecution are all beforeEach callbacks guaranteed to be done, whatever the registration order.
// The browser is only reachable through Playwright's internal impl class: may break on upgrade.
public class SessionExtension implements BeforeTestExecutionCallback, ParameterResolver, AfterTestExecutionCallback {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SessionExtension.class);

  @Override
  public void beforeTestExecution(ExtensionContext context) {
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    for (int index = 0; index < parameters.length; index++) {
      if (parameters[index].isAnnotationPresent(Session.class)) {
        SessionFixture<?> fixture = fixture(context, parameters[index], index);
        context.getStore(NAMESPACE).put(fixtureKey(index), fixture);
        context.getStore(NAMESPACE).put(sessionKey(index), fixture.create());
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    Class<?> type = parameterContext.getParameter().getType();
    return parameterContext.isAnnotated(Session.class)
        && (type == UserSession.class || type == GuestSession.class)
        && extensionContext.getTestMethod().filter(parameterContext.getDeclaringExecutable()::equals).isPresent();
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    Object session = extensionContext.getStore(NAMESPACE).get(sessionKey(parameterContext.getIndex()));
    if (session == null) {
      throw new ParameterResolutionException(
          "No session was created for parameter " + parameterContext.getIndex() + " of " + extensionContext.getRequiredTestMethod());
    }
    return session;
  }

  // not in the fixture's close(): the store closes after Allure has finished the test, and the attachment would be lost
  @Override
  public void afterTestExecution(ExtensionContext context) throws IOException {
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      SessionFixture<?> fixture = context.getStore(NAMESPACE).get(fixtureKey(i), SessionFixture.class);
      if (fixture == null || fixture.context() == null) {
        continue;
      }
      Traces.finish(fixture.context(), context, "trace-" + traceName(parameters[i], i) + ".zip");
    }
  }

  // without -parameters getName() returns a meaningless "arg1", so the position names the trace instead
  private static String traceName(Parameter parameter, int index) {
    return parameter.isNamePresent() ? parameter.getName() : "session-" + index;
  }

  private static SessionFixture<?> fixture(ExtensionContext context, Parameter parameter, int index) {
    if (!AnnotationSupport.isAnnotated(context.getRequiredTestClass(), UsePlaywright.class)) {
      throw new ExtensionConfigurationException("@Session needs a browser: annotate the test class with @UiTest");
    }
    boolean withUser = parameter.isAnnotationPresent(User.class);
    if (parameter.getType() == UserSession.class && withUser) {
      return new UserSessionFixture(browser(context), UserExtension.user(context, index));
    }
    if (parameter.getType() == GuestSession.class && !withUser) {
      return new GuestSessionFixture(browser(context));
    }
    throw new ExtensionConfigurationException(
        "@Session expects GuestSession without @User or UserSession with @User, got "
            + (withUser ? "@User " : "") + parameter.getType().getSimpleName()
            + " in parameter " + index + " of " + context.getRequiredTestMethod());
  }

  private static Browser browser(ExtensionContext context) {
    return BrowserExtension.getOrCreateBrowser(context);
  }

  private static String fixtureKey(int index) {
    return "session-fixture-" + index;
  }

  private static String sessionKey(int index) {
    return "session-" + index;
  }
}
