package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

@WebTest
@DisplayName("Registration")
class RegistrationTest {

  @Test
  @DisplayName("Newly registered user can log in and gets into the app under their own username")
  void newUserShouldBeAuthorizedAfterRegistration(Page page) {
    final String username = randomUsername();
    final String password = randomPassword();

    LandingPage.open(page)
        .clickRegister()
        .fillRegistrationForm(username, password, password)
        .submitRegistrationForm()
        .checkRegistrationSuccess()
        .clickSignIn()
        .clickLogin()
        .login(username, password)
        .header()
        .checkUserIsLoggedIn()
        .openProfile()
        .checkUsername(username);
  }
}
