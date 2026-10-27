package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.model.UserRequest;

import java.util.ArrayList;
import java.util.List;

import static io.student.rangiffler.fixture.Cleanup.runAll;

public final class UserWithOutcomeInvitationsFixture implements UserFixture {

  private final UserFixture delegate;
  private final int count;
  private final List<UserFixture> inviteeFixtures = new ArrayList<>();
  private final List<Friendship> invitations = new ArrayList<>();

  public UserWithOutcomeInvitationsFixture(UserFixture delegate, int count) {
    this.delegate = delegate;
    this.count = count;
  }

  @Override
  public TestUser create() {
    TestUser user = delegate.create();
    List<TestUser> invitees = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      UserFixture inviteeFixture = UserRequest.random().fixture();
      inviteeFixtures.add(inviteeFixture);
      TestUser invitee = inviteeFixture.create();
      Friendship invitation = Friendship.invitation(user.username(), invitee.username());
      Allure.step("Send invitation: " + user.username() + " -> " + invitee.username(), invitation::create);
      invitations.add(invitation);
      invitees.add(invitee);
    }
    return user.withOutcomeInvitations(invitees);
  }

  @Override
  public void close() {
    List<Runnable> cleanups = new ArrayList<>();
    invitations.forEach(invitation -> cleanups.add(invitation::delete));
    inviteeFixtures.forEach(fixture -> cleanups.add(fixture::close));
    cleanups.add(delegate::close);
    invitations.clear();
    inviteeFixtures.clear();
    runAll(cleanups);
  }
}
