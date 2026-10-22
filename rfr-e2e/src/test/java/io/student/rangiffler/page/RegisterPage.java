package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class RegisterPage extends BasePage<RegisterPage> {

  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator passwordSubmitInput;
  private final Locator submitButton;
  private final Locator successMessage;
  private final Locator signInLink;

  public RegisterPage(Page page) {
    super(page);
    this.usernameInput = page.locator("#username");
    this.passwordInput = page.locator("#password");
    this.passwordSubmitInput = page.locator("#passwordSubmit");
    this.submitButton = page.locator("#register-form .form__submit");
    this.successMessage = page.locator(".form__paragraph_success");
    this.signInLink = page.locator("a.form_sign-in");
  }

  @Override
  @Step("Check that registration page is loaded")
  public RegisterPage checkThatPageLoaded() {
    assertThat(page).hasURL(Pattern.compile("^" + Config.authUrl() + "/register"));
    assertThat(usernameInput).isVisible();
    assertThat(passwordInput).isVisible();
    assertThat(passwordSubmitInput).isVisible();
    return this;
  }

  @Step("Fill username: {username}")
  public RegisterPage fillUsername(String username) {
    usernameInput.fill(username);
    return this;
  }

  @Step("Fill password")
  public RegisterPage fillPassword(String password) {
    passwordInput.fill(password);
    return this;
  }

  @Step("Fill password confirmation")
  public RegisterPage fillPasswordSubmit(String password) {
    passwordSubmitInput.fill(password);
    return this;
  }

  @Step("Submit registration form")
  public RegisterPage submit() {
    submitButton.click();
    return this;
  }

  @Step("Register user {username}")
  public RegisterPage registerUser(String username, String password) {
    return fillUsername(username)
        .fillPassword(password)
        .fillPasswordSubmit(password)
        .submit();
  }

  @Step("Check that registration is successful")
  public RegisterPage checkRegistrationSuccess() {
    assertThat(successMessage).hasText("Congratulations! You've registered!");
    assertThat(signInLink).isVisible();
    return this;
  }

  /** "Sign in" leads back to the frontend landing page, login itself must start from there. */
  @Step("Click 'Sign in' after registration")
  public LandingPage clickSignIn() {
    signInLink.click();
    return new LandingPage(page).checkThatPageLoaded();
  }
}
