package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;
import io.student.rangiffler.page.component.PhotoCard;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class MyTravelsPage {

  public static final String URL = TestConfig.frontUrl() + "/my-travels";

  private final Page page;
  private final Header header;
  private final Locator travelsMapHeading;
  private final Locator photosHeading;
  private final Locator onlyMyTravelsButton;
  private final Locator withFriendsButton;
  private final Locator addPhotoButton;
  private final Locator countriesCounter;
  private final Locator countriesList;
  private final Locator resetCountryFilterButton;
  private final Locator photoCards;
  private final Locator peopleLink;

  public MyTravelsPage(Page page) {
    this.page = page;
    this.header = new Header(page);
    Locator main = page.getByRole(AriaRole.MAIN);
    this.travelsMapHeading = main.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Travels map").setExact(true));
    this.photosHeading = main.getByRole(AriaRole.HEADING, new Locator.GetByRoleOptions().setName("Photos").setExact(true));
    Locator travelsFilter = main.getByRole(AriaRole.GROUP, new Locator.GetByRoleOptions().setName("Travels filter").setExact(true));
    this.onlyMyTravelsButton = travelsFilter.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Only my travels").setExact(true));
    this.withFriendsButton = travelsFilter.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("With friends").setExact(true));
    this.addPhotoButton = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add photo").setExact(true));
    // while the countries list is open, the counter's accessible name becomes the list content: find it only with the list closed
    this.countriesCounter = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(Pattern.compile("^\\d+ countr(y|ies) visited$")));
    // the same country name is also on a map button: pick it inside the list
    this.countriesList = page.getByRole(AriaRole.TOOLTIP);
    this.resetCountryFilterButton = main.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Reset country filter").setExact(true));
    this.photoCards = main.getByRole(AriaRole.ARTICLE);
    this.peopleLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("People").setExact(true));
  }

  public Header header() {
    return header;
  }

  public PhotoCard photoCard(String description) {
    return new PhotoCard(photoCardLocator(description), description);
  }

  @Step("Open 'My travels' page")
  public static MyTravelsPage open(Page page) {
    page.navigate(URL);
    return new MyTravelsPage(page);
  }

  @Step("Go to People")
  public PeoplePage clickPeople() {
    return PeoplePage.openedBy(page, peopleLink::click);
  }

  @Step("Log out")
  public LandingPage clickLogout() {
    return header.clickLogout();
  }

  @Step("Show travels with friends")
  public MyTravelsPage showWithFriends() {
    withFriendsButton.click();
    return this;
  }

  @Step("Show only my travels")
  public MyTravelsPage showOnlyMyTravels() {
    onlyMyTravelsButton.click();
    return this;
  }

  @Step("Filter photos by country {country}")
  public MyTravelsPage filterByCountry(String country) {
    countriesCounter.click();
    countriesList.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(country).setExact(true)).click();
    return this;
  }

  @Step("Reset country filter")
  public MyTravelsPage resetCountryFilter() {
    resetCountryFilterButton.click();
    return this;
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

  @Step("Check that exactly photos {descriptions} are shown")
  public MyTravelsPage checkPhotos(List<String> descriptions) {
    assertThat(photoCards).hasCount(descriptions.size());
    for (String description : descriptions) {
      assertThat(photoCardLocator(description)).isVisible();
    }
    return this;
  }

  @Step("Check that the counter shows {count} visited countries")
  public MyTravelsPage checkCountriesVisited(int count) {
    assertThat(countriesCounter).hasText(count + (count == 1 ? " country" : " countries") + " visited");
    return this;
  }

  @Step("Check that photos are filtered by country {country}")
  public MyTravelsPage checkCountryFilter(String country) {
    assertThat(page.getByRole(AriaRole.MAIN).getByRole(AriaRole.BUTTON,
        new Locator.GetByRoleOptions().setName("Country filter: " + country).setExact(true))).isVisible();
    return this;
  }

  private Locator photoCardLocator(String description) {
    return photoCards.filter(new Locator.FilterOptions().setHasText(description));
  }
}
