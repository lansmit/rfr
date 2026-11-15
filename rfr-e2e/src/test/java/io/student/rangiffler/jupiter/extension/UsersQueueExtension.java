package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.fixture.StaticUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import io.student.rangiffler.model.TestUser;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class UsersQueueExtension implements ParameterResolver {

  private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UsersQueueExtension.class);

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(StaticUser.class)
        && parameterContext.getParameter().getType().isAssignableFrom(TestUser.class);
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    UserFixture fixture = new StaticUserFixture();
    extensionContext.getStore(NAMESPACE).put(parameterContext.getIndex(), fixture);
    return fixture.create();
  }
}
