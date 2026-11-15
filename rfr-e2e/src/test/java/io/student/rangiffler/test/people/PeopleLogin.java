package io.student.rangiffler.test.people;

import com.microsoft.playwright.Page;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.page.LandingPage;
import io.student.rangiffler.page.PeoplePage;

final class PeopleLogin {

  private PeopleLogin() {
  }

  static PeoplePage open(Page page, TestUser user) {
    return LandingPage.open(page)
        .clickLogin()
        .login(user.username(), user.password())
        .clickPeople();
  }
}
