package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

@WebTest
@DisplayName("Login: negative")
class LoginNegativeTest {

  // guards against a user stored with a password that matches anything (e.g. a broken hash prefix)
  @Test
  @DisplayName("Existing user cannot log in with a wrong password")
  void userShouldNotBeAuthorizedWithWrongPassword(Page page, @User TestUser user) {
    LandingPage.open(page)
        .clickLogin()
        .fillLoginForm(user.username(), user.password() + "x")
        .submitLoginForm()
        .checkLoginError("Bad credentials");
  }

  // the message is the same as for a wrong password, so the form doesn't reveal which usernames exist
  @Test
  @DisplayName("Non-existent user cannot log in")
  void nonExistentUserShouldNotBeAuthorized(Page page) {
    LandingPage.open(page)
        .clickLogin()
        .fillLoginForm(randomUsername(), randomPassword())
        .submitLoginForm()
        .checkLoginError("Bad credentials");
  }
}
