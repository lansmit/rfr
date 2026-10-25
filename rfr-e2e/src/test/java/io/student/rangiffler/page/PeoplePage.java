package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeoplePage {

  public static final String URL = TestConfig.frontUrl() + "/people";

  private final Page page;
  private final Header header;
  private final Locator main;
  private final Locator friendsTab;
  private final Locator allPeopleTab;
  private final Locator outcomeInvitationsTab;
  private final Locator incomeInvitationsTab;
  private final Locator searchInput;
  private final Locator searchButton;
  private final Locator rows;

  public PeoplePage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.main = page.getByRole(AriaRole.MAIN);
    this.friendsTab = tab("Friends");
    this.allPeopleTab = tab("All People");
    this.outcomeInvitationsTab = tab("Outcome invitations");
    this.incomeInvitationsTab = tab("Income invitations");
    this.searchInput = main.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("search people").setExact(true));
    this.searchButton = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("search").setExact(true));
    this.rows = main.getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL)));
  }

  public Header header() {
    return header;
  }

  @Step("Open people page")
  public static PeoplePage open(Page page) {
    page.navigate(URL);
    return new PeoplePage(page);
  }

  @Step("Reload people page")
  public PeoplePage reload() {
    page.reload();
    return this;
  }

  @Step("Open 'Friends' tab")
  public PeoplePage openFriendsTab() {
    friendsTab.click();
    return this;
  }

  @Step("Open 'All People' tab")
  public PeoplePage openAllPeopleTab() {
    allPeopleTab.click();
    return this;
  }

  @Step("Open 'Outcome invitations' tab")
  public PeoplePage openOutcomeInvitationsTab() {
    outcomeInvitationsTab.click();
    return this;
  }

  @Step("Open 'Income invitations' tab")
  public PeoplePage openIncomeInvitationsTab() {
    incomeInvitationsTab.click();
    return this;
  }

  @Step("Search people: {query}")
  public PeoplePage searchPeople(String query) {
    searchInput.fill(query);
    searchButton.click();
    return this;
  }

  @Step("Send invitation to {username}")
  public PeoplePage sendInvitation(String username) {
    Locator addButton = row(username).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add").setExact(true));
    page.waitForResponse(PeoplePage::isFriendshipActionResponse, addButton::click);
    return this;
  }

  @Step("Check that friends are exactly {usernames}")
  public PeoplePage checkFriends(List<String> usernames) {
    return checkPeople(usernames, "Remove");
  }

  @Step("Check that income invitations are exactly from {usernames}")
  public PeoplePage checkIncomeInvitations(List<String> usernames) {
    return checkPeople(usernames, "Accept");
  }

  @Step("Check that outcome invitations are exactly to {usernames}")
  public PeoplePage checkOutcomeInvitations(List<String> usernames) {
    return checkPeople(usernames, "Cancel");
  }

  private PeoplePage checkPeople(List<String> usernames, String actionButton) {
    assertThat(rows).hasCount(usernames.size());
    for (String username : usernames) {
      assertThat(row(username).getByRole(AriaRole.BUTTON,
          new Locator.GetByRoleOptions().setName(actionButton).setExact(true))).isVisible();
    }
    return this;
  }

  private Locator tab(String name) {
    return page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(name).setExact(true));
  }

  private Locator row(String username) {
    return rows.filter(new Locator.FilterOptions().setHas(
        page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(username).setExact(true))));
  }

  private static boolean isFriendshipActionResponse(Response response) {
    String body = response.request().postData();
    return response.url().endsWith("/graphql") && body != null && body.contains("\"operationName\":\"FriendshipAction\"");
  }
}
