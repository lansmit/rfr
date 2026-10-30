package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RegisterPage {

  public static final String URL = TestConfig.authUrl() + "/register";

  private final Page page;
  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator passwordSubmitInput;
  private final Locator submitButton;
  private final Locator successMessage;
  private final Locator signInLink;

  public RegisterPage(Page page) {
    this.page = page;
    this.usernameInput = page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
    this.passwordInput = page.getByLabel("Password", new Page.GetByLabelOptions().setExact(true));
    this.passwordSubmitInput = page.getByLabel("Confirm password", new Page.GetByLabelOptions().setExact(true));
    this.submitButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up").setExact(true));
    this.successMessage = page.locator(".form__paragraph_success");
    this.signInLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in").setExact(true));
  }

  @Step("Fill registration form: username={username}, password={password}, passwordSubmit={passwordSubmit}")
  public RegisterPage fillRegistrationForm(String username, String password, String passwordSubmit) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    passwordSubmitInput.fill(passwordSubmit);
    return this;
  }

  @Step("Submit registration form")
  public RegisterPage submitRegistrationForm() {
    submitButton.click();
    return this;
  }

  /** "Sign in" leads back to the frontend landing page, login itself must start from there. */
  @Step("Click 'Sign in' after registration")
  public LandingPage clickSignIn() {
    signInLink.click();
    return new LandingPage(page);
  }

  @Step("Check that registration is successful")
  public RegisterPage checkRegistrationSuccess() {
    assertThat(successMessage).hasText("Congratulations! You've registered!");
    assertThat(signInLink).isVisible();
    return this;
  }
}
