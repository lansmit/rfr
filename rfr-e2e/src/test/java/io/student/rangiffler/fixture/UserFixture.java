package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.ApiUserRepository;
import io.student.rangiffler.data.AuthUserRepository;
import io.student.rangiffler.model.TestUser;

public final class UserFixture implements AutoCloseable {

  private final AuthUserRepository authUserRepository = new AuthUserRepository();
  private final ApiUserRepository apiUserRepository = new ApiUserRepository();
  private final String username;
  private final String password;
  private TestUser user;

  public UserFixture(String username, String password) {
    this.username = username;
    this.password = password;
  }

  public TestUser create() {
    if (user != null) {
      throw new IllegalStateException("User " + username + " is already created");
    }
    user = Allure.step("Create user in auth DB: username=" + username + ", password=" + password,
        () -> authUserRepository.create(username, password));
    return user;
  }

  @Override
  public void close() {
    if (user == null) {
      return;
    }
    TestUser created = user;
    user = null;
    // the API profile appears only after the first login, so for some tests there is nothing to delete there
    runAll(
        () -> apiUserRepository.deleteByUsername(created.username()),
        () -> authUserRepository.delete(created.id())
    );
  }

  // each cleanup is independent: a failure in one must not skip the rest or hide their errors
  private static void runAll(Runnable... cleanups) {
    RuntimeException failure = null;
    for (Runnable cleanup : cleanups) {
      try {
        cleanup.run();
      } catch (RuntimeException e) {
        if (failure == null) {
          failure = e;
        } else {
          failure.addSuppressed(e);
        }
      }
    }
    if (failure != null) {
      throw failure;
    }
  }
}
