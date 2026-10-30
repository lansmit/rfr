package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.UserToRegister;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WebTest
@DisplayName("Registration")
class RegistrationTest {

  @Test
  @DisplayName("Newly registered user can log in and gets into the app under their own username")
  void newUserShouldBeAuthorizedAfterRegistration(Page page, @UserToRegister TestUser user) {
    LandingPage.open(page)
        .clickRegister()
        .fillRegistrationForm(user.username(), user.password(), user.password())
        .submitRegistrationForm()
        .checkRegistrationSuccess()
        .clickSignIn()
        .clickLogin()
        .login(user.username(), user.password())
        .header()
        .checkUserIsLoggedIn()
        .openProfile()
        .checkUsername(user.username());
  }
}
