package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.model.UserRequest;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class UserExtension implements ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(User.class)
        && parameterContext.getParameter().getType().isAssignableFrom(TestUser.class);
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    UserFixture fixture = UserRequest.from(parameterContext.findAnnotation(User.class).orElseThrow()).fixture();
    // JUnit closes AutoCloseable store values when the test method's context ends, i.e. after the test
    extensionContext.getStore(NAMESPACE).put(parameterContext.getIndex(), fixture);
    return fixture.create();
  }
}
