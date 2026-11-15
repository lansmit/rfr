package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.page.GraphQl;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeopleRow {

  private final Locator self;
  private final String username;
  private final Locator buttons;
  private final Locator addButton;
  private final Locator waitingLabel;

  public PeopleRow(Locator self, String username) {
    this.self = self;
    this.username = username;
    this.buttons = self.getByRole(AriaRole.BUTTON);
    this.addButton = self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add").setExact(true));
    this.waitingLabel = self.getByText("Waiting...", new Locator.GetByTextOptions().setExact(true));
  }

  @Step("Send invitation to {this.username}")
  public PeopleRow sendInvitation() {
    self.page().waitForResponse(response -> GraphQl.isOperationResponse(response, "FriendshipAction"), addButton::click);
    return this;
  }

  @Step("Check that {this.username} is a friend")
  public PeopleRow checkFriend() {
    assertThat(buttons).hasText(new String[]{"Remove"});
    return this;
  }

  @Step("Check that {this.username} sent an income invitation")
  public PeopleRow checkIncomeInvitation() {
    assertThat(buttons).hasText(new String[]{"Accept", "Decline"});
    return this;
  }

  @Step("Check that an outcome invitation to {this.username} is waiting")
  public PeopleRow checkOutcomeInvitation() {
    assertThat(waitingLabel).isVisible();
    assertThat(buttons).hasText(new String[]{"Cancel"});
    return this;
  }
}
