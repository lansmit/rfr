package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserToRegisterFixture;
import io.student.rangiffler.jupiter.annotation.UserToRegister;
import io.student.rangiffler.model.TestUser;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class UserToRegisterExtension implements ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserToRegisterExtension.class);

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(UserToRegister.class)
        && parameterContext.getParameter().getType().isAssignableFrom(TestUser.class);
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    UserFixture fixture = new UserToRegisterFixture();
    extensionContext.getStore(NAMESPACE).put(parameterContext.getIndex(), fixture);
    return fixture.create();
  }
}
