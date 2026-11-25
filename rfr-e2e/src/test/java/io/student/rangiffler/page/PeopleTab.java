package io.student.rangiffler.page;

import com.microsoft.playwright.Response;
import io.student.rangiffler.network.AppResponses;
import java.util.function.Predicate;

public enum PeopleTab {
  FRIENDS("Friends", AppResponses.friendsLoaded()),
  ALL_PEOPLE("All People", AppResponses.allPeopleLoaded()),
  OUTCOME_INVITATIONS("Outcome invitations", AppResponses.outcomeInvitationsLoaded()),
  INCOME_INVITATIONS("Income invitations", AppResponses.incomeInvitationsLoaded());

  private final String title;
  private final Predicate<Response> loaded;

  PeopleTab(String title, Predicate<Response> loaded) {
    this.title = title;
    this.loaded = loaded;
  }

  public String title() {
    return title;
  }

  Predicate<Response> loaded() {
    return loaded;
  }

  @Override
  public String toString() {
    return title;
  }
}
