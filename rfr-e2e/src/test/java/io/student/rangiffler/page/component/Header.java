package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.ProfilePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class Header {

  private final Page page;
  private final Locator profileLink;
  private final Locator logoutButton;

  public Header(Page page) {
    this.page = page;
    Locator self = page.getByRole(AriaRole.BANNER);
    this.profileLink = self.getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile").setExact(true));
    this.logoutButton = self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Logout").setExact(true));
  }

  @Step("Open profile from header")
  public ProfilePage openProfile() {
    profileLink.click();
    return new ProfilePage(page);
  }

  @Step("Log out from Header")
  public LandingPage clickLogout() {
    logoutButton.click();
    return new LandingPage(page);
  }

  @Step("Check that header of an authenticated user is shown")
  public Header checkUserIsLoggedIn() {
    assertAll(
        () -> assertThat(profileLink).isVisible(),
        () -> assertThat(logoutButton).isVisible()
    );
    return this;
  }
}
