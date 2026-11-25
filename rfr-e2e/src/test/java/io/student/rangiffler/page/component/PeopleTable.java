package io.student.rangiffler.page.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import org.junit.jupiter.api.function.Executable;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public final class PeopleTable {

  private final Page page;
  private final Locator rows;
  private final Locator emptyMessage;
  private final Locator loader;
  private final Locator loadError;

  public PeopleTable(Page page) {
    this.page = page;
    Locator main = page.getByRole(AriaRole.MAIN);
    this.rows = main.getByTestId("people-row");
    this.emptyMessage = main.getByText("There are no users yet", new Locator.GetByTextOptions().setExact(true));
    this.loadError = main.getByRole(AriaRole.ALERT);
    this.loader = main.getByRole(AriaRole.PROGRESSBAR, new Locator.GetByRoleOptions().setName("Loading users").setExact(true));
  }

  public PeopleRow row(String username) {
    return new PeopleRow(rowLocator(username), username);
  }

  @Step("Check that the table shows exactly {usernames}")
  public PeopleTable checkUsernames(List<String> usernames) {
    List<Executable> checks = new ArrayList<>();
    checks.add(() -> assertThat(rows).hasCount(usernames.size()));
    for (String username : usernames) {
      checks.add(() -> assertThat(rowLocator(username)).isVisible());
    }
    assertAll("People table does not show exactly the users " + usernames, checks);
    return this;
  }

  @Step("Check that the table is empty")
  public PeopleTable checkEmpty() {
    assertAll("People table is not empty",
        () -> assertThat(rows).hasCount(0),
        () -> assertThat(emptyMessage).isVisible()
    );
    return this;
  }

  @Step("Check that the table is loading")
  public PeopleTable checkLoading() {
    assertAll("People table does not show that users are loading",
        () -> assertThat(loader).isVisible(),
        () -> assertThat(rows).hasCount(0),
        () -> assertThat(emptyMessage).isHidden()
    );
    return this;
  }

  @Step("Check that the table failed to load with error: {message}")
  public PeopleTable checkLoadError(String message) {
    assertThat(loadError).containsText(message);
    return this;
  }

  @Step("Check that the table finished loading")
  public PeopleTable checkLoaded() {
    assertThat(loader).isHidden();
    return this;
  }

  private Locator rowLocator(String username) {
    return rows.filter(new Locator.FilterOptions().setHas(
        page.getByText(username, new Page.GetByTextOptions().setExact(true))));
  }
}
