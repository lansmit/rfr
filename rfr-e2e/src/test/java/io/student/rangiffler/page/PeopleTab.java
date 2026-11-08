package io.student.rangiffler.page;

public enum PeopleTab {
  FRIENDS("Friends", "GetFriends"),
  ALL_PEOPLE("All People", "GetPeople"),
  OUTCOME_INVITATIONS("Outcome invitations", "GetOutcomeInvitations"),
  INCOME_INVITATIONS("Income invitations", "GetInvitations");

  private final String title;
  private final String operationName;

  PeopleTab(String title, String operationName) {
    this.title = title;
    this.operationName = operationName;
  }

  public String title() {
    return title;
  }

  public String operationName() {
    return operationName;
  }
}
