package io.student.rangiffler.session;

import com.microsoft.playwright.Page;
import io.student.rangiffler.model.TestUser;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.page.MyTravelsPage;

import java.util.Optional;

// the fixture leaves the page on 'My travels' after login, so this is an entry point, not a navigation
public record UserSession(TestUser user, Page page, Optional<GqlStubs> stubs) {

  public MyTravelsPage myTravelsPage() {
    return new MyTravelsPage(page);
  }
}
