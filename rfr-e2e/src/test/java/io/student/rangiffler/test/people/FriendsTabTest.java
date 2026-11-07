package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Route;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.annotation.WebTest;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.PeoplePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@WebTest
@DisplayName("People: Friends tab")
class FriendsTabTest {

  @Test
  @DisplayName("Friends tab shows only friends with Remove button")
  void friendsTabShouldShowOnlyFriends(Page page, @User(friends = 1, incomeInvitations = 1) TestUser user) {
    login(page, user)
        .checkFriends(usernames(user.friends()));
  }

  @Test
  @DisplayName("Friends tab is empty when there are no friends")
  void friendsTabShouldBeEmptyWithoutFriends(Page page, @User(incomeInvitations = 1) TestUser user) {
    login(page, user)
        .checkNoPeople();
  }

  @Test
  @DisplayName("Opening the already selected Friends tab keeps showing friends")
  void openingSelectedFriendsTabShouldKeepFriends(Page page, @User(friends = 1) TestUser user) {
    login(page, user)
        .openFriendsTab()
        .checkFriends(usernames(user.friends()));
  }

  @Test
  @DisplayName("Empty Friends tab check fails when the API answers GetFriends with an error")
  void emptyCheckShouldFailOnApiError(Page page, @User TestUser user) {
    page.route("**/graphql", route -> {
      String body = route.request().postData();
      if (body != null && body.contains("\"operationName\":\"GetFriends\"")) {
        route.fulfill(new Route.FulfillOptions()
            .setContentType("application/json")
            .setBody("{\"errors\":[{\"message\":\"injected\",\"extensions\":{\"classification\":\"INTERNAL_ERROR\"}}]}"));
      } else {
        route.resume();
      }
    });
    PeoplePage peoplePage = login(page, user);

    assertThrows(AssertionError.class, peoplePage::checkNoPeople);
  }

  private static List<String> usernames(List<TestUser> users) {
    return users.stream().map(TestUser::username).toList();
  }

  private static PeoplePage login(Page page, TestUser user) {
    LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .header()
        .checkUserIsLoggedIn();
    return PeoplePage.open(page);
  }
}
