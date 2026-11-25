package io.student.rangiffler.session;

import com.microsoft.playwright.Page;
import io.student.rangiffler.network.GqlStubs;
import io.student.rangiffler.page.LandingPage;

import java.util.Optional;

// the fixture leaves the page on the landing page, so this is an entry point, not a navigation
public record GuestSession(Page page, Optional<GqlStubs> stubs) {

  public LandingPage landingPage() {
    return new LandingPage(page);
  }
}
