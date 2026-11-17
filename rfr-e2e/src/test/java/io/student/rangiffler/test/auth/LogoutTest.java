package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.PeopleTab;
import io.student.rangiffler.session.UserSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

@UiTest
@DisplayName("Logout")
class LogoutTest {

  // the friends list needs a fresh API call with the session's token; the profile could come from the Apollo cache,
  // and an empty tab looks the same as a server error
  @Test
  @DisplayName("Logout in one context keeps the session in another context logged in")
  void logoutInOneContextKeepsTheSessionInAnother(Page page, @Session @User(friends = 1) UserSession bob) {
    LandingPage.open(page)
        .clickLogin()
        .login(bob.user().username(), bob.user().password())
        .clickLogout()
        .checkUserIsNotLoggedIn();

    bob.myTravelsPage()
        .clickPeople()
        .openTab(PeopleTab.FRIENDS)
        .table()
        .checkUsernames(List.of("no-such-friend"));
  }
}
