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
@DisplayName("People: Income invitations tab")
class IncomeInvitationsTabTest {

  @Test
  @DisplayName("Income invitations tab shows only income invitations with Accept and Decline buttons")
  void incomeInvitationsTabShouldShowOnlyIncomeInvitations(Page page, @User(friends = 1, incomeInvitations = 1) TestUser user) {
    PeopleTable table = login(page, user)
        .openTab(PeopleTab.INCOME_INVITATIONS)
        .table();
    List<String> inviters = usernames(user.incomeInvitations());
    table.checkUsernames(inviters);
    inviters.forEach(inviter -> table.row(inviter).checkIncomeInvitation());
  }

  @Test
  @DisplayName("Income invitations tab is empty when there are no income invitations")
  void incomeInvitationsTabShouldBeEmptyWithoutIncomeInvitations(Page page, @User(friends = 1) TestUser user) {
    login(page, user)
        .openTab(PeopleTab.INCOME_INVITATIONS)
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
