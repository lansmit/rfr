package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.PeopleTab;
import io.student.rangiffler.page.PeoplePage;
import io.student.rangiffler.page.component.PeopleTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@UiTest
@DisplayName("People: friendship preconditions")
class FriendshipPreconditionsTest {

  @Test
  @DisplayName("User created with friends sees exactly them in the friends list")
  void userShouldSeeFriendsFromPrecondition(Page page, @User(friends = 2) TestUser user) {
    PeopleTable table = login(page, user).table();
    List<String> friends = usernames(user.friends());
    table.checkUsernames(friends);
    friends.forEach(friend -> table.row(friend).checkFriend());
  }

  @Test
  @DisplayName("User created with income invitations sees exactly them in income invitations")
  void userShouldSeeIncomeInvitationsFromPrecondition(Page page, @User(incomeInvitations = 2) TestUser user) {
    PeopleTable table = login(page, user)
        .openTab(PeopleTab.INCOME_INVITATIONS)
        .table();
    List<String> inviters = usernames(user.incomeInvitations());
    table.checkUsernames(inviters);
    inviters.forEach(inviter -> table.row(inviter).checkIncomeInvitation());
  }

  @Test
  @DisplayName("User created with outcome invitations sees exactly them in outcome invitations")
  void userShouldSeeOutcomeInvitationsFromPrecondition(Page page, @User(outcomeInvitations = 2) TestUser user) {
    PeopleTable table = login(page, user)
        .openTab(PeopleTab.OUTCOME_INVITATIONS)
        .table();
    List<String> invitees = usernames(user.outcomeInvitations());
    table.checkUsernames(invitees);
    invitees.forEach(invitee -> table.row(invitee).checkOutcomeInvitation());
  }

  @Test
  @DisplayName("Friends and income invitations created together are shown in their own tabs")
  void userShouldSeeFriendsAndIncomeInvitationsFromPrecondition(Page page,
                                                                @User(friends = 1, incomeInvitations = 1) TestUser user) {
    String friend = user.friends().getFirst().username();
    String inviter = user.incomeInvitations().getFirst().username();
    PeoplePage peoplePage = login(page, user);
    peoplePage.table()
        .checkUsernames(List.of(friend))
        .row(friend)
        .checkFriend();
    peoplePage.openTab(PeopleTab.INCOME_INVITATIONS)
        .table()
        .checkUsernames(List.of(inviter))
        .row(inviter)
        .checkIncomeInvitation();
  }

  @Test
  @DisplayName("Invitation sent from the browser by a user without preconditions is saved")
  void invitationSentInBrowserShouldBeSaved(Page page, @User TestUser user, @User TestUser invitee) {
    PeoplePage peoplePage = login(page, user)
        .openTab(PeopleTab.ALL_PEOPLE)
        .searchPeople(invitee.username());
    peoplePage.table()
        .row(invitee.username())
        .sendInvitation();
    peoplePage.reload()
        .openTab(PeopleTab.OUTCOME_INVITATIONS)
        .table()
        .checkUsernames(List.of(invitee.username()))
        .row(invitee.username())
        .checkOutcomeInvitation();
  }

  private static List<String> usernames(List<TestUser> users) {
    return users.stream().map(TestUser::username).toList();
  }

  private static PeoplePage login(Page page, TestUser user) {
    LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .header()
        .checkUserIsLoggedIn();
    return PeoplePage.open(page);
  }
}
