package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.Config;
import io.student.rangiffler.page.component.Header;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class MyTravelsPage extends BasePage<MyTravelsPage> {

  private final Header header;
  private final Locator travelsMapTitle;

  public MyTravelsPage(Page page) {
    super(page);
    this.header = new Header(page);
    this.travelsMapTitle = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Travels map"));
  }

  public Header header() {
    return header;
  }

  @Override
  @Step("Check that 'My travels' page is loaded")
  public MyTravelsPage checkThatPageLoaded() {
    assertThat(page).hasURL(Config.frontUrl() + "/my-travels");
    assertThat(travelsMapTitle).isVisible();
    return this;
  }
}
