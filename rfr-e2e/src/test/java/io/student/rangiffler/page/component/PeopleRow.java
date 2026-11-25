package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.domain.FriendStatus;
import io.student.rangiffler.domain.PeopleAction;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class PeopleRow {

  private final Locator self;
  private final String username;
  private final Locator buttons;
  private final Locator waitingLabel;

  public PeopleRow(Locator self, String username) {
    this.self = self;
    this.username = username;
    this.buttons = self.getByRole(AriaRole.BUTTON);
    this.waitingLabel = self.getByText("Waiting...", new Locator.GetByTextOptions().setExact(true));
  }

  @Step("Click Add for {this.username}")
  public PeopleRow clickAdd() {
    button(PeopleAction.ADD).click();
    return this;
  }

  @Step("Click Accept for {this.username}")
  public PeopleRow clickAccept() {
    button(PeopleAction.ACCEPT).click();
    return this;
  }

  @Step("Click Decline for {this.username}")
  public PeopleRow clickDecline() {
    button(PeopleAction.DECLINE).click();
    return this;
  }

  @Step("Click Cancel for {this.username}")
  public PeopleRow clickCancel() {
    button(PeopleAction.CANCEL).click();
    return this;
  }

  @Step("Click Remove for {this.username}")
  public PeopleRow clickRemove() {
    button(PeopleAction.REMOVE).click();
    return this;
  }

  @Step("Check that {this.username} has status {status}")
  public PeopleRow checkStatus(FriendStatus status) {
    assertAll("Row of " + username + " does not match status " + status,
        () -> assertThat(buttons).hasText(status.actions().stream().map(PeopleAction::title).toArray(String[]::new)),
        () -> {
          if (status.waiting()) {
            assertThat(waitingLabel).isVisible();
          } else {
            assertThat(waitingLabel).isHidden();
          }
        }
    );
    return this;
  }

  private Locator button(PeopleAction action) {
    return self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(action.title()).setExact(true));
  }
}
