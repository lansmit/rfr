package io.student.rangiffler.test.auth;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@WebTest
@DisplayName("Login")
class LoginTest {

  private static final String FIXED_PASSWORD = "qwerty123";

  @Test
  @DisplayName("User with random credentials can log in and gets into the app under their own username")
  void userWithRandomCredentialsShouldBeAuthorized(Page page, @User TestUser user) {
    LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .header()
        .checkUserIsLoggedIn()
        .openProfile()
        .checkUsername(user.username());
  }

  // logs in with the literal, not user.password(), to prove the annotation value reached the DB
  @Test
  @DisplayName("User created with a given password can log in with exactly that password")
  void userWithGivenPasswordShouldBeAuthorized(Page page, @User(password = FIXED_PASSWORD) TestUser user) {
    LandingPage.open(page)
        .clickLogin()
        .login(user.username(), FIXED_PASSWORD)
        .header()
        .checkUserIsLoggedIn()
        .openProfile()
        .checkUsername(user.username());
  }
}
