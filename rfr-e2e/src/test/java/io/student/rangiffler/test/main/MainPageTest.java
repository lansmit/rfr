package io.student.rangiffler.test.main;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WebTest
@DisplayName("Main page")
class MainPageTest {

  @Test
  @DisplayName("Logged-in user sees the main page with travels map, photos and add photo button")
  void mainPageShouldBeShownAfterLogin(Page page, @User TestUser user) {
    LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .checkIsOpened()
        .checkMainElements()
        .header()
        .checkUserIsLoggedIn();
  }
}
