package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.AuthAccount;
import io.student.rangiffler.data.db.Profile;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.model.UserRequest;

import static io.student.rangiffler.fixture.Cleanup.runAll;

public final class SingleUserFixture implements UserFixture {

  private final UserRequest request;
  private AuthAccount account;
  private Profile profile;

  public SingleUserFixture(UserRequest request) {
    this.request = request;
  }

  @Override
  public TestUser create() {
    if (account != null) {
      throw new IllegalStateException("User " + request.username() + " is already created");
    }
    AuthAccount newAccount = AuthAccount.of(request.username(), request.password());
    Allure.step("Create user in auth DB: username=" + request.username() + ", password=" + request.password(),
        newAccount::create);
    account = newAccount;
    profile = new Profile(request.username());
    Allure.step("Create profile in API DB: username=" + request.username(), profile::create);
    return new TestUser(account.id(), account.username(), account.password());
  }

  @Override
  public void close() {
    if (account == null) {
      return;
    }
    AuthAccount createdAccount = account;
    Profile createdProfile = profile;
    account = null;
    profile = null;
    runAll(createdProfile::delete, createdAccount::delete);
  }
}
