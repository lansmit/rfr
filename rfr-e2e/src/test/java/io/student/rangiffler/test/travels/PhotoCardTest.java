package io.student.rangiffler.test.travels;

import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("My travels: photo card")
class PhotoCardTest {

  // photos are mocks in rfr-api: every user sees the same feed
  private static final String MOCK_PHOTO_DESCRIPTION = "Париж!";

  @Test
  @DisplayName("Own photo card has Photo actions and like buttons")
  void ownPhotoCardShouldHaveActionsAndLikeButtons(Page page, @User TestUser user) {
    LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .photoCard(MOCK_PHOTO_DESCRIPTION)
        .checkOwnPhotoButtons();
  }
}
