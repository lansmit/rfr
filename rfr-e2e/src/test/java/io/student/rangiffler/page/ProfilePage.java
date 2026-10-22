package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {

  public static final String URL = TestConfig.frontUrl() + "/profile";

  private final Page page;
  private final Header header;
  private final Locator usernameInput;

  public ProfilePage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.usernameInput = page.getByLabel("Username", new Page.GetByLabelOptions().setExact(true));
  }

  public Header header() {
    return header;
  }

  @Step("Open profile page")
  public static ProfilePage open(Page page) {
    page.navigate(URL);
    return new ProfilePage(page);
  }

  // username is resolved by the API from the JWT, so a match proves the session belongs to this user
  @Step("Check that profile username is {username}")
  public ProfilePage checkUsername(String username) {
    assertThat(usernameInput).hasValue(username);
    return this;
  }
}
