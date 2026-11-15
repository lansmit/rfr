package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.mock.MockFeed;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.MyTravelsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("My travels on the mock feed")
class MyTravelsMockTest {

  private static final MockFeed ONLY_MINE = MockFeed.onlyMine();
  private static final MockFeed WITH_FRIENDS = MockFeed.withFriends();

  @Test
  @DisplayName("Travels filter switches photos and visited countries counter on the mock feed")
  void travelsFilterShouldSwitchPhotosAndCounter(Page page, @User TestUser user) {
    login(page, user)
        .checkPhotos(ONLY_MINE.descriptions())
        .checkCountriesVisited(ONLY_MINE.countriesVisited())
        .showWithFriends()
        .checkPhotos(WITH_FRIENDS.descriptions())
        .checkCountriesVisited(WITH_FRIENDS.countriesVisited())
        .showOnlyMyTravels()
        .checkPhotos(ONLY_MINE.descriptions())
        .checkCountriesVisited(ONLY_MINE.countriesVisited());
  }

  @Test
  @DisplayName("Country filter shows only photos of the country and reset brings all photos back on the mock feed")
  void countryFilterShouldShowOnlyCountryPhotos(Page page, @User TestUser user) {
    String country = WITH_FRIENDS.friendsPhotos().getFirst().country();
    login(page, user)
        .showWithFriends()
        .checkPhotos(WITH_FRIENDS.descriptions())
        .filterByCountry(country)
        .checkCountryFilter(country)
        .checkPhotos(WITH_FRIENDS.descriptionsIn(country))
        .resetCountryFilter()
        .checkPhotos(WITH_FRIENDS.descriptions());
  }

  @Test
  @DisplayName("Photo actions menu is shown only on own photos on the mock feed")
  void photoActionsShouldBeOnlyOnOwnPhotos(Page page, @User TestUser user) {
    MyTravelsPage myTravelsPage = login(page, user)
        .showWithFriends()
        .checkPhotos(WITH_FRIENDS.descriptions());
    WITH_FRIENDS.ownPhotos().forEach(photo -> myTravelsPage.photoCard(photo.description()).checkOwnPhotoButtons());
    WITH_FRIENDS.friendsPhotos().forEach(photo -> myTravelsPage.photoCard(photo.description()).checkNoPhotoActions());
  }

  private static MyTravelsPage login(Page page, TestUser user) {
    return LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password());
  }
}
