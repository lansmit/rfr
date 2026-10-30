package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.model.TestUser;

import static io.student.rangiffler.fixture.Cleanup.runAll;
import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

// the test registers this user itself through the UI; the fixture only owns the cleanup
public final class UserToRegisterFixture implements UserFixture {

  private TestUser user;

  @Override
  public TestUser create() {
    if (user != null) {
      throw new IllegalStateException("User " + user.username() + " is already created");
    }
    user = new TestUser(null, randomUsername(), randomPassword());
    return user;
  }

  @Override
  public void close() {
    if (user == null) {
      return;
    }
    String username = user.username();
    user = null;
    Allure.step("Delete registered user from both DBs: username=" + username,
        () -> runAll(new Profile(username)::delete, AuthAccount.of(username, "")::deleteByUsername));
  }
}
