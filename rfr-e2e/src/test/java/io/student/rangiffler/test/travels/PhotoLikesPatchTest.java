package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.mock.MockFeed;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.network.AwaitedGqlOperation;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.LoginPage;
import io.student.rangiffler.page.MyTravelsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomLikesCount;

@UiTest
@DisplayName("My travels: likes patched in the GetFeed response")
class PhotoLikesPatchTest {

  private static final MockFeed ONLY_MINE = MockFeed.onlyMine();
  private static final String PATCHED = "Париж!";
  private static final String NEIGHBOUR = "Нью-Йорк!";

  @Test
  @DisplayName("Patched likes count is shown on its card, the neighbour card keeps its real count")
  void patchedLikesShouldChangeOnlyTheirCard(Page page, @User TestUser user, GqlStubs stubs) {
    int likes = randomLikesCount();
    stubs.patch("GetFeed", "$.data.feed.photos.edgs[?(@.node.description == '" + PATCHED + "')].node.likes.total", likes);

    LoginPage loginPage = LandingPage.open(page).clickLogin();
    new AwaitedGqlOperation(page, "GetFeed", () -> loginPage.login(user.username(), user.password())).await();
    MyTravelsPage myTravelsPage = new MyTravelsPage(page);
    myTravelsPage.photoCard(PATCHED).checkLikes(likes);
    myTravelsPage.photoCard(NEIGHBOUR).checkLikes(ONLY_MINE.photo(NEIGHBOUR).likes());
  }
}
