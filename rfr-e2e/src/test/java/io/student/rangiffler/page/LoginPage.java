package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * A bare {@code /login} redirects back to the frontend: open it via {@link LandingPage#clickLogin()},
 * which goes through {@code /oauth2/authorize}.
 */
public class LoginPage extends BasePage<LoginPage> {

  private final Locator usernameInput;
  private final Locator passwordInput;
  private final Locator submitButton;

  public LoginPage(Page page) {
    super(page);
    this.usernameInput = page.locator("input[name=username]");
    this.passwordInput = page.locator("input[name=password]");
    this.submitButton = page.locator("form .form__submit");
  }

  @Override
  @Step("Check that login page is loaded")
  public LoginPage checkThatPageLoaded() {
    assertThat(page).hasURL(Pattern.compile("^" + Config.authUrl() + "/login"));
    assertThat(usernameInput).isVisible();
    assertThat(passwordInput).isVisible();
    return this;
  }

  @Step("Login as {username}")
  public MyTravelsPage login(String username, String password) {
    usernameInput.fill(username);
    passwordInput.fill(password);
    submitButton.click();
    return new MyTravelsPage(page).checkThatPageLoaded();
  }
}
