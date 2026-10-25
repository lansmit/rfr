package io.student.rangiffler.model;

import io.student.rangiffler.fixture.SingleUserFixture;
import io.student.rangiffler.fixture.UserFixture;
import io.student.rangiffler.fixture.UserWithFriendsFixture;
import io.student.rangiffler.fixture.UserWithIncomeInvitationsFixture;
import io.student.rangiffler.jupiter.annotation.User;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

public record UserRequest(String username, String password, int friends, int incomeInvitations) {

  public static UserRequest from(User annotation) {
    return new UserRequest(
        annotation.username().isEmpty() ? randomUsername() : annotation.username(),
        annotation.password().isEmpty() ? randomPassword() : annotation.password(),
        annotation.friends(),
        annotation.incomeInvitations());
  }

  public static UserRequest random() {
    return new UserRequest(randomUsername(), randomPassword(), 0, 0);
  }

  public UserFixture fixture() {
    UserFixture fixture = new SingleUserFixture(this);
    if (friends > 0) {
      fixture = new UserWithFriendsFixture(fixture, friends);
    }
    if (incomeInvitations > 0) {
      fixture = new UserWithIncomeInvitationsFixture(fixture, incomeInvitations);
    }
    return fixture;
  }
}
