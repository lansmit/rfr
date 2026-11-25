package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.model.UserRequest;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.lang.reflect.Parameter;

// The only creator of @User users, whatever the parameter type: other extensions (SessionExtension) take them through user().
public class UserExtension implements BeforeEachCallback, ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

  @Override
  public void beforeEach(ExtensionContext context) {
    Parameter[] parameters = context.getRequiredTestMethod().getParameters();
    for (int index = 0; index < parameters.length; index++) {
      User annotation = parameters[index].getAnnotation(User.class);
      if (annotation != null) {
        UserFixture fixture = UserRequest.from(annotation).fixture();
        // JUnit closes AutoCloseable store values when the test method's context ends, i.e. after the test
        context.getStore(NAMESPACE).put(fixtureKey(index), fixture);
        context.getStore(NAMESPACE).put(userKey(index), fixture.create());
      }
    }
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(User.class)
        && parameterContext.getParameter().getType() == TestUser.class
        && extensionContext.getTestMethod().filter(parameterContext.getDeclaringExecutable()::equals).isPresent();
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return user(extensionContext, parameterContext.getIndex());
  }

  public static TestUser user(ExtensionContext context, int parameterIndex) {
    TestUser user = context.getStore(NAMESPACE).get(userKey(parameterIndex), TestUser.class);
    if (user == null) {
      throw new ParameterResolutionException(
          "No user was created for parameter " + parameterIndex + " of " + context.getRequiredTestMethod());
    }
    return user;
  }

  private static String fixtureKey(int index) {
    return "user-fixture-" + index;
  }

  private static String userKey(int index) {
    return "user-" + index;
  }
}
