package io.student.rangiffler.model;

import java.util.List;
import java.util.UUID;

public record TestUser(UUID id, String username, String password,
                       List<TestUser> friends, List<TestUser> incomeInvitations, List<TestUser> outcomeInvitations) {

  public TestUser(UUID id, String username, String password) {
    this(id, username, password, List.of(), List.of(), List.of());
  }

  public TestUser withFriends(List<TestUser> friends) {
    return new TestUser(id, username, password, List.copyOf(friends), incomeInvitations, outcomeInvitations);
  }

  public TestUser withIncomeInvitations(List<TestUser> incomeInvitations) {
    return new TestUser(id, username, password, friends, List.copyOf(incomeInvitations), outcomeInvitations);
  }

  public TestUser withOutcomeInvitations(List<TestUser> outcomeInvitations) {
    return new TestUser(id, username, password, friends, incomeInvitations, List.copyOf(outcomeInvitations));
  }
}
