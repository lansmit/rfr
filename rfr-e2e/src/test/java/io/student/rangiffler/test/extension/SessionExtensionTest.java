package io.student.rangiffler.test.extension;

import io.student.rangiffler.jupiter.annotation.Session;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.session.GuestSession;
import io.student.rangiffler.session.UserSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UiTest
@DisplayName("SessionExtension")
class SessionExtensionTest {

  @Test
  @DisplayName("A guest browser stays logged out while another browser in the same test is logged in")
  void guestBrowserShouldNotShareLogin(@Session @User UserSession user, @Session GuestSession guest) {
    user.mainPage()
        .header()
        .checkUserIsLoggedIn();
    guest.landingPage()
        .checkUserIsNotLoggedIn();
  }
}
