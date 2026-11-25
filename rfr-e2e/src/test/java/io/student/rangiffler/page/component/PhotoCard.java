package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class PhotoCard {

  private final String description;
  private final Locator actionsButton;
  private final Locator likeButton;
  private final Locator deleteMenuItem;

  public PhotoCard(Locator self, String description) {
    this.description = description;
    this.actionsButton = self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Photo actions").setExact(true));
    this.likeButton = self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("like").setExact(true));
    this.deleteMenuItem = self.page().getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName("Delete").setExact(true));
  }

  @Step("Delete photo '{this.description}'")
  public PhotoCard deletePhoto() {
    actionsButton.click();
    deleteMenuItem.click();
    return this;
  }

  @Step("Check that photo '{this.description}' has actions and like buttons")
  public PhotoCard checkOwnPhotoButtons() {
    assertAll(
        () -> assertThat(actionsButton).isVisible(),
        () -> assertThat(likeButton).isVisible()
    );
    return this;
  }

  @Step("Check that photo '{this.description}' has like button but no actions")
  public PhotoCard checkNoPhotoActions() {
    assertAll(
        () -> assertThat(likeButton).isVisible(),
        () -> assertThat(actionsButton).hasCount(0)
    );
    return this;
  }
}
