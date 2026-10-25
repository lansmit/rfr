package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.model.UserRequest;

import java.util.ArrayList;
import java.util.List;

import static io.student.rangiffler.fixture.Cleanup.runAll;

public final class UserWithFriendsFixture implements UserFixture {

  private final UserFixture delegate;
  private final int count;
  private final List<UserFixture> friendFixtures = new ArrayList<>();
  private final List<Friendship> friendships = new ArrayList<>();

  public UserWithFriendsFixture(UserFixture delegate, int count) {
    this.delegate = delegate;
    this.count = count;
  }

  @Override
  public TestUser create() {
    TestUser user = delegate.create();
    List<TestUser> friends = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      UserFixture friendFixture = UserRequest.random().fixture();
      friendFixtures.add(friendFixture);
      TestUser friend = friendFixture.create();
      Friendship friendship = Friendship.friends(friend.username(), user.username());
      Allure.step("Make friends: " + friend.username() + " and " + user.username(), friendship::create);
      friendships.add(friendship);
      friends.add(friend);
    }
    return user.withFriends(friends);
  }

  @Override
  public void close() {
    List<Runnable> cleanups = new ArrayList<>();
    friendships.forEach(friendship -> cleanups.add(friendship::delete));
    friendFixtures.forEach(fixture -> cleanups.add(fixture::close));
    cleanups.add(delegate::close);
    friendships.clear();
    friendFixtures.clear();
    runAll(cleanups);
  }
}
