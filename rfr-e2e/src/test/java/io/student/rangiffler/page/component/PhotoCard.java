package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PhotoCard {

  private final String description;
  private final Locator actionsButton;
  private final Locator likeButton;

  public PhotoCard(Locator self, String description) {
    this.description = description;
    this.actionsButton = self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Photo actions").setExact(true));
    this.likeButton = self.page().getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("like").setExact(true));
  }

  @Step("Check that photo '{this.description}' has actions and like buttons")
  public PhotoCard checkOwnPhotoButtons() {
    assertThat(actionsButton).isVisible();
    assertThat(likeButton).isVisible();
    return this;
  }
}
