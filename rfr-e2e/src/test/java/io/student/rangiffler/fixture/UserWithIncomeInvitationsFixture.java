package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.model.UserRequest;

import java.util.ArrayList;
import java.util.List;

import static io.student.rangiffler.fixture.Cleanup.runAll;

public final class UserWithIncomeInvitationsFixture implements UserFixture {

  private final UserFixture delegate;
  private final int count;
  private final List<UserFixture> inviterFixtures = new ArrayList<>();
  private final List<Friendship> invitations = new ArrayList<>();

  public UserWithIncomeInvitationsFixture(UserFixture delegate, int count) {
    this.delegate = delegate;
    this.count = count;
  }

  @Override
  public TestUser create() {
    TestUser user = delegate.create();
    List<TestUser> inviters = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      UserFixture inviterFixture = UserRequest.random().fixture();
      inviterFixtures.add(inviterFixture);
      TestUser inviter = inviterFixture.create();
      Friendship invitation = Friendship.invitation(inviter.username(), user.username());
      Allure.step("Send invitation: " + inviter.username() + " -> " + user.username(), invitation::create);
      invitations.add(invitation);
      inviters.add(inviter);
    }
    return user.withIncomeInvitations(inviters);
  }

  @Override
  public void close() {
    List<Runnable> cleanups = new ArrayList<>();
    invitations.forEach(invitation -> cleanups.add(invitation::delete));
    inviterFixtures.forEach(fixture -> cleanups.add(fixture::close));
    cleanups.add(delegate::close);
    invitations.clear();
    inviterFixtures.clear();
    runAll(cleanups);
  }
}
