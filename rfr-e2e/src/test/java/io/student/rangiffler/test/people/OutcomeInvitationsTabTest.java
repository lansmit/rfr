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
@DisplayName("People: Outcome invitations tab")
class OutcomeInvitationsTabTest {

  @Test
  @DisplayName("Outcome invitations tab shows only outcome invitations with Waiting... and Cancel")
  void outcomeInvitationsTabShouldShowOnlyOutcomeInvitations(Page page, @User(friends = 1, outcomeInvitations = 1) TestUser user) {
    login(page, user)
        .openOutcomeInvitationsTab()
        .checkOutcomeInvitations(usernames(user.outcomeInvitations()));
  }

  @Test
  @DisplayName("Outcome invitations tab is empty when there are no outcome invitations")
  void outcomeInvitationsTabShouldBeEmptyWithoutOutcomeInvitations(Page page, @User(friends = 1) TestUser user) {
    login(page, user)
        .openOutcomeInvitationsTab()
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
