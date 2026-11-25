package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.data.mock.MockFeed;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.MyTravelsPage;
import io.student.rangiffler.page.component.Notification;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("My travels: delete photo")
class PhotoDeleteTest {

  private static final MockFeed ONLY_MINE = MockFeed.onlyMine();

  @Test
  @Disabled("Не реализовано в rfr-api")
  @DisplayName("Deleted photo disappears from the feed after reload")
  void deletedPhotoShouldDisappearAfterReload(Page page, @User TestUser user, GqlStubs stubs) {
    int photosBefore = ONLY_MINE.photos().size();
    String description = ONLY_MINE.ownPhotos().getFirst().description();

    MyTravelsPage myTravelsPage = LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .checkPhotosCount(photosBefore);
    stubs.respond("GetFeed", "gql/feed-without-amsterdam.json");
    myTravelsPage.photoCard(description).deletePhoto();
    myTravelsPage.checkNotification(Notification.PHOTO_DELETED)
        .reload()
        .checkPhotosCount(photosBefore - 1);
  }
}
