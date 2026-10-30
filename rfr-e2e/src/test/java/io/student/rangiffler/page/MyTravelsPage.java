package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class MyTravelsPage {

  public static final String URL = TestConfig.frontUrl() + "/my-travels";

  private final Page page;
  private final Header header;
  private final Locator travelsMapHeading;
  private final Locator photosHeading;
  private final Locator onlyMyTravelsButton;
  private final Locator addPhotoButton;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.header = new Header(page);
    this.travelsMapHeading = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Travels map").setExact(true));
    this.photosHeading = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName("Photos").setExact(true));
    Locator travelsFilter = page.getByRole(AriaRole.GROUP, new Page.GetByRoleOptions().setName("Travels filter").setExact(true));
    this.onlyMyTravelsButton = travelsFilter.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Only my travels").setExact(true));
    this.addPhotoButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add photo").setExact(true));
  }

  public Header header() {
    return header;
  }

  @Step("Open 'My travels' page")
  public static MyTravelsPage open(Page page) {
    page.navigate(URL);
    return new MyTravelsPage(page);
  }

  @Step("Check that 'My travels' page is opened")
  public MyTravelsPage checkIsOpened() {
    assertThat(page).hasURL(URL);
    return this;
  }

  @Step("Check main elements of 'My travels' page")
  public MyTravelsPage checkMainElements() {
    assertThat(travelsMapHeading).isVisible();
    assertThat(photosHeading).isVisible();
    assertThat(addPhotoButton).isVisible();
    assertThat(onlyMyTravelsButton).hasAttribute("aria-pressed", "true");
    return this;
  }
}
