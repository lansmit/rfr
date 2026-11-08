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
@DisplayName("People: Income invitations tab")
class IncomeInvitationsTabTest {

  @Test
  @DisplayName("Income invitations tab shows only income invitations with Accept and Decline buttons")
  void incomeInvitationsTabShouldShowOnlyIncomeInvitations(Page page, @User(friends = 1, incomeInvitations = 1) TestUser user) {
    login(page, user)
        .openIncomeInvitationsTab()
        .checkIncomeInvitations(usernames(user.incomeInvitations()));
  }

  @Test
  @DisplayName("Income invitations tab is empty when there are no income invitations")
  void incomeInvitationsTabShouldBeEmptyWithoutIncomeInvitations(Page page, @User(friends = 1) TestUser user) {
    login(page, user)
        .openIncomeInvitationsTab()
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
