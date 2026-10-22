package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.ProfilePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Header {

  private final Page page;
  private final Locator profileButton;
  private final Locator logoutButton;

  public Header(Page page) {
    this.page = page;
    Locator self = page.getByRole(AriaRole.BANNER);
    this.profileButton = self.getByLabel("Profile");
    this.logoutButton = self.getByLabel("Logout");
  }

  @Step("Check that header of an authenticated user is shown")
  public Header checkUserIsLoggedIn() {
    assertThat(profileButton).isVisible();
    assertThat(logoutButton).isVisible();
    return this;
  }

  @Step("Open profile from header")
  public ProfilePage openProfile() {
    profileButton.click();
    return new ProfilePage(page).checkThatPageLoaded();
  }
}
