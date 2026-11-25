package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.student.rangiffler.fixture.SessionFixture;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.network.GqlStubs;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;
import java.util.Arrays;

public class GqlStubsExtension implements ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(GqlStubsExtension.class);
  private static final String STUBS_KEY = "stubs";

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType() == GqlStubs.class
        && extensionContext.getTestMethod().filter(parameterContext.getDeclaringExecutable()::equals).isPresent();
  }

  @Override
  public GqlStubs resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    Parameter[] parameters = extensionContext.getRequiredTestMethod().getParameters();
    long stubsParameters = Arrays.stream(parameters).filter(parameter -> parameter.getType() == GqlStubs.class).count();
    if (stubsParameters > 1) {
      throw new ExtensionConfigurationException(
          "A test takes at most one GqlStubs parameter, for its Page; stubs of a @Session context come with "
              + "@Session(useStubs = true) in the session record. Got " + stubsParameters + " in "
              + extensionContext.getRequiredTestMethod());
    }
    SessionFixture<?> session = null;
    for (int index = 0; index < parameters.length; index++) {
      if (parameters[index].isAnnotationPresent(Session.class)) {
        session = extensionContext.getStore(ExtensionContext.Namespace.create(SessionExtension.class))
            .get("session-fixture-" + index, SessionFixture.class);
      }
    }
    GqlStubs stubs = new GqlStubs(session.context());
    extensionContext.getStore(NAMESPACE).put(STUBS_KEY, stubs);
    return stubs;
  }
}
