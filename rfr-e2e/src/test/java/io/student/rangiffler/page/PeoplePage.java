package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;
import io.student.rangiffler.page.component.PeopleTable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class PeoplePage {

  public static final String URL = TestConfig.frontUrl() + "/people";
  private static final ObjectMapper JSON = new ObjectMapper();

  private final Page page;
  private final Header header;
  private final PeopleTable table;
  private final Locator searchInput;
  private final Locator searchButton;
  private Response peopleResponse;

  public PeoplePage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.table = new PeopleTable(page);

    Locator main = page.getByRole(AriaRole.MAIN);

    this.searchInput = main.getByRole(AriaRole.TEXTBOX, new Locator.GetByRoleOptions().setName("search people").setExact(true));
    this.searchButton = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("search").setExact(true));
  }

  public Header header() {
    return header;
  }

  public PeopleTable table() {
    return table;
  }

  @Step("Open people page")
  public static PeoplePage open(Page page) {
    PeoplePage peoplePage = new PeoplePage(page);
    peoplePage.peopleResponse = page.waitForResponse(response -> GraphQl.isOperationResponse(response, "GetFriends"), () -> page.navigate(URL));
    return peoplePage;
  }

  @Step("Reload people page")
  public PeoplePage reload() {
    peopleResponse = page.waitForResponse(response -> GraphQl.isOperationResponse(response, "GetFriends"), () -> page.reload());
    return this;
  }

  // a click on the already selected tab doesn't remount the table, so no request would come
  @Step("Open '{tab.title}' tab")
  public PeoplePage openTab(PeopleTab tab) {
    Locator tabLocator = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(tab.title()).setExact(true));
    if (!"true".equals(tabLocator.getAttribute("aria-selected"))) {
      peopleResponse = page.waitForResponse(response -> GraphQl.isOperationResponse(response, tab.operationName()), tabLocator::click);
    }
    return this;
  }

  @Step("Search people: {query}")
  public PeoplePage searchPeople(String query) {
    searchInput.fill(query);
    searchButton.click();
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
    table.checkEmpty();
    return this;
  }
}
