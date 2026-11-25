package io.student.rangiffler.session;

import com.microsoft.playwright.Page;
import io.student.rangiffler.page.LandingPage;

// the fixture leaves the page on the landing page, so this is an entry point, not a navigation
public record GuestSession(Page page) {

  public LandingPage landingPage() {
    return new LandingPage(page);
  }
}
