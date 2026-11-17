package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import java.util.Arrays;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

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

  @Step("Check that {this.username} is a friend")
  public PeopleRow checkFriend() {
    return checkActions(PeopleAction.REMOVE);
  }

  @Step("Check that {this.username} sent an income invitation")
  public PeopleRow checkIncomeInvitation() {
    return checkActions(PeopleAction.ACCEPT, PeopleAction.DECLINE);
  }

  @Step("Check that an outcome invitation to {this.username} is waiting")
  public PeopleRow checkOutcomeInvitation() {
    assertThat(waitingLabel).isVisible();
    return checkActions(PeopleAction.CANCEL);
  }

  private PeopleRow checkActions(PeopleAction... actions) {
    assertThat(buttons).hasText(Arrays.stream(actions).map(PeopleAction::title).toArray(String[]::new));
    return this;
  }

  private Locator button(PeopleAction action) {
    return self.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(action.title()).setExact(true));
  }
}
