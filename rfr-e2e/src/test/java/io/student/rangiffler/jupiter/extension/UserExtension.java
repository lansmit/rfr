package io.student.rangiffler.jupiter.extension;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.AuthUserRepository;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

public class UserExtension implements ParameterResolver {

  private final AuthUserRepository authUserRepository = new AuthUserRepository();

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.isAnnotated(User.class)
        && parameterContext.getParameter().getType().isAssignableFrom(TestUser.class);
  }

  @Override
  public TestUser resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
    User annotation = parameterContext.findAnnotation(User.class).orElseThrow();
    String username = annotation.username().isEmpty() ? randomUsername() : annotation.username();
    String password = annotation.password().isEmpty() ? randomPassword() : annotation.password();
    return Allure.step("Create user in auth DB: username=" + username + ", password=" + password,
        () -> authUserRepository.create(username, password));
  }
}
