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
@DisplayName("People: Outcome invitations tab")
class OutcomeInvitationsTabTest {

  @Test
  @DisplayName("Outcome invitations tab shows only outcome invitations with Waiting... and Cancel")
  void outcomeInvitationsTabShouldShowOnlyOutcomeInvitations(Page page, @User(friends = 1, outcomeInvitations = 1) TestUser user) {
    PeopleTable table = login(page, user)
        .openTab(PeopleTab.OUTCOME_INVITATIONS)
        .table();
    List<String> invitees = usernames(user.outcomeInvitations());
    table.checkUsernames(invitees);
    invitees.forEach(invitee -> table.row(invitee).checkOutcomeInvitation());
  }

  @Test
  @DisplayName("Outcome invitations tab is empty when there are no outcome invitations")
  void outcomeInvitationsTabShouldBeEmptyWithoutOutcomeInvitations(Page page, @User(friends = 1) TestUser user) {
    login(page, user)
        .openTab(PeopleTab.OUTCOME_INVITATIONS)
        .checkNoPeople();
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
