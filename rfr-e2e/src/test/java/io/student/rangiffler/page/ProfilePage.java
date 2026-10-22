package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ProfilePage extends BasePage<ProfilePage> {

  private final Locator usernameInput;

  public ProfilePage(Page page) {
    super(page);
    this.usernameInput = page.locator("#username");
  }

  @Override
  @Step("Check that profile page is loaded")
  public ProfilePage checkThatPageLoaded() {
    assertThat(page).hasURL(Config.frontUrl() + "/profile");
    assertThat(usernameInput).isVisible();
    return this;
  }

  // username is resolved by the API from the JWT, so a match proves the session belongs to this user
  @Step("Check that profile belongs to {username}")
  public ProfilePage checkUsername(String username) {
    assertThat(usernameInput).hasValue(username);
    return this;
  }
}
