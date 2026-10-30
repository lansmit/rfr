package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * A bare {@code /login} redirects back to the frontend: open it via {@link LandingPage#clickLogin()},
 * which goes through {@code /oauth2/authorize}.
 */
public final class LoginPage {

  public static final String URL = TestConfig.authUrl() + "/login";

  private final Page page;
  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator submitButton;
  private final Locator errorMessage;

  public LoginPage(Page page) {
    this.page = page;
    this.usernameInput = page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
    this.passwordInput = page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true));
    this.submitButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
    this.errorMessage = page.locator(".form__error");
  }

  @Step("Fill login form: username={username}, password={password}")
  public LoginPage fillLoginForm(String username, String password) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    return this;
  }

  @Step("Submit login form")
  public LoginPage submitLoginForm() {
    submitButton.click();
    return this;
  }

  @Step("Login as {username} with password {password}")
  public MyTravelsPage login(String username, String password) {
    fillLoginForm(username, password).submitLoginForm();
    return new MyTravelsPage(page);
  }

  @Step("Check that login error is {message}")
  public LoginPage checkLoginError(String message) {
    assertThat(errorMessage).hasText(message);
    return this;
  }
}
