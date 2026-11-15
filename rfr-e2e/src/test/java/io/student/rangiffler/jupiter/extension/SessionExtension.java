package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.impl.junit.BrowserExtension;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.fixture.GuestSessionFixture;
import io.student.rangiffler.fixture.SessionFixture;
import io.student.rangiffler.fixture.UserSessionFixture;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.UserRequest;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;

// The browser is only reachable through Playwright's internal impl class: may break on upgrade.
public class SessionExtension implements ParameterResolver, AfterEachCallback {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(SessionExtension.class);

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    Class<?> type = parameterContext.getParameter().getType();
    return parameterContext.isAnnotated(Session.class)
        && (type == UserSession.class || type == GuestSession.class)
        && extensionContext.getTestMethod().filter(parameterContext.getDeclaringExecutable()::equals).isPresent();
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    if (!AnnotationSupport.isAnnotated(extensionContext.getRequiredTestClass(), UsePlaywright.class)) {
      throw new ParameterResolutionException("@Session needs a browser: annotate the test class with @UiTest");
    }
    SessionFixture<?> fixture = fixture(parameterContext, BrowserExtension.getOrCreateBrowser(extensionContext));
    extensionContext.getStore(NAMESPACE).put(parameterContext.getIndex(), fixture);
    return fixture.create();
  }

  // runs before the store closes the fixtures, so every session context is still open
  @Override
  public void afterEach(ExtensionContext context) throws IOException {
    boolean keep = Traces.shouldKeep(context);
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    for (int i = 0; i < parameters.length; i++) {
      SessionFixture<?> fixture = context.getStore(NAMESPACE).get(i, SessionFixture.class);
      if (fixture == null) {
        continue;
      }
      if (keep) {
        Path trace = Traces.path(context, "trace-" + traceName(parameters[i], i) + ".zip");
        fixture.saveTrace(trace);
        Traces.attach(trace);
      } else {
        fixture.discardTrace();
      }
    }
  }

  // without -parameters getName() returns a meaningless "arg1", so the position names the trace instead
  private static String traceName(Parameter parameter, int index) {
    return parameter.isNamePresent() ? parameter.getName() : "session-" + index;
  }

  private static SessionFixture<?> fixture(ParameterContext parameterContext, Browser browser) {
    Optional<User> user = parameterContext.findAnnotation(User.class);
    if (parameterContext.getParameter().getType() == UserSession.class) {
      User annotation = user.orElseThrow(() -> new ParameterResolutionException(
          "UserSession needs a user: add @User or take a GuestSession"));
      return new UserSessionFixture(browser, UserRequest.from(annotation).fixture());
    }
    if (user.isPresent()) {
      throw new ParameterResolutionException("GuestSession has no user: remove @User or take a UserSession");
    }
    return new GuestSessionFixture(browser);
  }
}
