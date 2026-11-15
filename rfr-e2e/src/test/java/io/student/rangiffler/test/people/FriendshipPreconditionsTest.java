package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.PeoplePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@WebTest
@DisplayName("People: friendship preconditions")
class FriendshipPreconditionsTest {

  @Test
  @DisplayName("User created with friends sees exactly them in the friends list")
  void userShouldSeeFriendsFromPrecondition(Page page, @User(friends = 2) TestUser user) {
    login(page, user)
        .checkFriends(usernames(user.friends()));
  }

  @Test
  @DisplayName("User created with income invitations sees exactly them in income invitations")
  void userShouldSeeIncomeInvitationsFromPrecondition(Page page, @User(incomeInvitations = 2) TestUser user) {
    login(page, user)
        .openIncomeInvitationsTab()
        .checkIncomeInvitations(usernames(user.incomeInvitations()));
  }

  @Test
  @DisplayName("User created with outcome invitations sees exactly them in outcome invitations")
  void userShouldSeeOutcomeInvitationsFromPrecondition(Page page, @User(outcomeInvitations = 2) TestUser user) {
    login(page, user)
        .openOutcomeInvitationsTab()
        .checkOutcomeInvitations(usernames(user.outcomeInvitations()));
  }

  @Test
  @DisplayName("Friends and income invitations created together are shown in their own tabs")
  void userShouldSeeFriendsAndIncomeInvitationsFromPrecondition(Page page,
                                                                @User(friends = 1, incomeInvitations = 1) TestUser user) {
    login(page, user)
        .checkFriends(usernames(user.friends()))
        .openIncomeInvitationsTab()
        .checkIncomeInvitations(usernames(user.incomeInvitations()));
  }

  @Test
  @DisplayName("Invitation sent from the browser by a user without preconditions is saved")
  void invitationSentInBrowserShouldBeSaved(Page page, @User TestUser user, @User TestUser invitee) {
    login(page, user)
        .openAllPeopleTab()
        .searchPeople(invitee.username())
        .sendInvitation(invitee.username())
        .reload()
        .openOutcomeInvitationsTab()
        .checkOutcomeInvitations(List.of(invitee.username()));
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
