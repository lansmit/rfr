package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.network.AppResponses;
import io.student.rangiffler.page.component.Header;
import io.student.rangiffler.page.component.Notification;
import io.student.rangiffler.page.component.PeopleTable;
import io.student.rangiffler.page.component.Snackbar;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class PeoplePage {

  public static final String URL = TestConfig.frontUrl() + "/people";

  private final Page page;
  private final Header header;
  private final PeopleTable table;
  private final Snackbar snackbar;
  private final Locator searchInput;
  private final Locator searchButton;
  private Response peopleResponse;

  public PeoplePage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.table = new PeopleTable(page);
    this.snackbar = new Snackbar(page);

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
    return openedBy(page, () -> page.navigate(URL));
  }

  // the page opens on the Friends tab: its list is the response checkNoPeople() reads
  static PeoplePage openedBy(Page page, Runnable navigation) {
    PeoplePage peoplePage = new PeoplePage(page);
    peoplePage.peopleResponse = page.waitForResponse(AppResponses.friendsLoaded(), navigation);
    return peoplePage;
  }

  @Step("Reload people page")
  public PeoplePage reload() {
    peopleResponse = page.waitForResponse(AppResponses.friendsLoaded(), () -> page.reload());
    return this;
  }

  // a click on the already selected tab doesn't remount the table, so no request would come
  @Step("Open '{tab.title}' tab")
  public PeoplePage openTab(PeopleTab tab) {
    Locator tabLocator = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(tab.title()).setExact(true));
    if (!"true".equals(tabLocator.getAttribute("aria-selected"))) {
      peopleResponse = page.waitForResponse(tab.loaded(), tabLocator::click);
    }
    return this;
  }

  @Step("Click '{tab.title}' tab without waiting for its list")
  public PeoplePage clickTab(PeopleTab tab) {
    page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(tab.title()).setExact(true)).click();
    return this;
  }

  @Step("Search people: {query}")
  public PeoplePage searchPeople(String query) {
    searchInput.fill(query);
    searchButton.click();
    return this;
  }

  @Step("Check notification on People page: {notification.text}")
  public PeoplePage checkNotification(Notification notification) {
    snackbar.checkShown(notification);
    return this;
  }

  @Step("Check that people list is empty")
  public PeoplePage checkNoPeople() {
    assertNotNull(peopleResponse, "people list was not loaded through open(), reload() or a tab method");
    AppResponses.checkPeopleListEmpty(peopleResponse);
    table.checkEmpty();
    return this;
  }
}
