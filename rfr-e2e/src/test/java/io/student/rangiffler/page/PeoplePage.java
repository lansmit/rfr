package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class PeoplePage {

  public static final String URL = TestConfig.frontUrl() + "/people";
  private static final ObjectMapper JSON = new ObjectMapper();

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
  private final Locator emptyMessage;
  private Response peopleResponse;

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
    this.emptyMessage = main.getByText("There are no users yet", new Locator.GetByTextOptions().setExact(true));
  }

  public Header header() {
    return header;
  }

  @Step("Open people page")
  public static PeoplePage open(Page page) {
    PeoplePage peoplePage = new PeoplePage(page);
    peoplePage.peopleResponse = page.waitForResponse(response -> isOperationResponse(response, "GetFriends"), () -> page.navigate(URL));
    return peoplePage;
  }

  @Step("Reload people page")
  public PeoplePage reload() {
    peopleResponse = page.waitForResponse(response -> isOperationResponse(response, "GetFriends"), () -> page.reload());
    return this;
  }

  @Step("Open 'Friends' tab")
  public PeoplePage openFriendsTab() {
    return openTab(friendsTab, "GetFriends");
  }

  @Step("Open 'All People' tab")
  public PeoplePage openAllPeopleTab() {
    return openTab(allPeopleTab, "GetPeople");
  }

  @Step("Open 'Outcome invitations' tab")
  public PeoplePage openOutcomeInvitationsTab() {
    return openTab(outcomeInvitationsTab, "GetOutcomeInvitations");
  }

  @Step("Open 'Income invitations' tab")
  public PeoplePage openIncomeInvitationsTab() {
    return openTab(incomeInvitationsTab, "GetInvitations");
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
    page.waitForResponse(response -> isOperationResponse(response, "FriendshipAction"), addButton::click);
    return this;
  }

  @Step("Check that friends are exactly {usernames}")
  public PeoplePage checkFriends(List<String> usernames) {
    return checkPeople(usernames, "Remove");
  }

  @Step("Check that income invitations are exactly from {usernames}")
  public PeoplePage checkIncomeInvitations(List<String> usernames) {
    return checkPeople(usernames, "Accept", "Decline");
  }

  @Step("Check that outcome invitations are exactly to {usernames}")
  public PeoplePage checkOutcomeInvitations(List<String> usernames) {
    checkPeople(usernames, "Cancel");
    for (String username : usernames) {
      assertThat(row(username).getByText("Waiting...", new Locator.GetByTextOptions().setExact(true))).isVisible();
    }
    return this;
  }

  @Step("Check that people list is empty")
  public PeoplePage checkNoPeople() {
    assertNotNull(peopleResponse, "people list was not loaded through open(), reload() or a tab method");
    JsonNode body = JSON.readTree(peopleResponse.text());
    assertFalse(body.has("errors"), "GraphQL returned errors: " + body.get("errors"));
    JsonNode edges = body.findValue("edges");
    assertNotNull(edges, "no people list in the response: " + body);
    assertEquals(0, edges.size(), "people list is not empty: " + edges);
    assertThat(rows).hasCount(0);
    assertThat(emptyMessage).isVisible();
    return this;
  }

  private PeoplePage checkPeople(List<String> usernames, String... actionButtons) {
    assertThat(rows).hasCount(usernames.size());
    for (String username : usernames) {
      for (String actionButton : actionButtons) {
        assertThat(row(username).getByRole(AriaRole.BUTTON,
            new Locator.GetByRoleOptions().setName(actionButton).setExact(true))).isVisible();
      }
    }
    return this;
  }

  // a click on the already selected tab doesn't remount the table, so no request would come
  private PeoplePage openTab(Locator tab, String operationName) {
    if (!"true".equals(tab.getAttribute("aria-selected"))) {
      peopleResponse = page.waitForResponse(response -> isOperationResponse(response, operationName), tab::click);
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

  private static boolean isOperationResponse(Response response, String operationName) {
    String body = response.request().postData();
    return response.url().endsWith("/graphql") && body != null && body.contains("\"operationName\":\"" + operationName + "\"");
  }
}
