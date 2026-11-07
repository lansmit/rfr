package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.model.UserRequest;

import java.util.ArrayList;
import java.util.List;

import static io.student.rangiffler.fixture.Cleanup.runAll;

public final class UserWithRelationsFixture implements UserFixture {

  private final UserFixture delegate;
  private final Relation relation;
  private final int count;
  private final List<UserFixture> partnerFixtures = new ArrayList<>();
  private final List<Friendship> friendships = new ArrayList<>();

  public UserWithRelationsFixture(UserFixture delegate, Relation relation, int count) {
    this.delegate = delegate;
    this.relation = relation;
    this.count = count;
  }

  @Override
  public TestUser create() {
    TestUser user = delegate.create();
    List<TestUser> partners = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      UserFixture partnerFixture = UserRequest.random().fixture();
      partnerFixtures.add(partnerFixture);
      TestUser partner = partnerFixture.create();
      Friendship friendship = relation.friendship(user.username(), partner.username());
      Allure.step("Create friendship: " + friendship.requester() + " -> " + friendship.addressee()
          + " (" + friendship.status() + ")", friendship::create);
      friendships.add(friendship);
      partners.add(partner);
    }
    return relation.attach(user, partners);
  }

  @Override
  public void close() {
    List<Runnable> cleanups = new ArrayList<>();
    friendships.forEach(friendship -> cleanups.add(friendship::delete));
    partnerFixtures.forEach(fixture -> cleanups.add(fixture::close));
    cleanups.add(delegate::close);
    friendships.clear();
    partnerFixtures.clear();
    runAll(cleanups);
  }
}
