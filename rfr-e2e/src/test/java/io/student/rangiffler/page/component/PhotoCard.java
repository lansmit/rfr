package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class PhotoCard {

  private final String description;
  private final Locator actionsButton;
  private final Locator likeButton;

  public PhotoCard(Locator self, String description) {
    this.description = description;
    this.actionsButton = self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Photo actions").setExact(true));
    this.likeButton = self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("like").setExact(true));
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
    // the like button proves the card is found, otherwise the missing actions button would pass for a missing card
    assertAll(
        () -> assertThat(likeButton).isVisible(),
        () -> assertThat(actionsButton).hasCount(0)
    );
    return this;
  }
}
