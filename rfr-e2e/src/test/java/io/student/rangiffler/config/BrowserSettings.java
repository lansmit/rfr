package io.student.rangiffler.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;

public final class BrowserSettings {

  private BrowserSettings() {
  }

  public static Browser.NewContextOptions contextOptions() {
    return new Browser.NewContextOptions()
        .setViewportSize(TestConfig.viewportWidth(), TestConfig.viewportHeight())
        // keeps error messages in English on any machine (e.g. one with a Russian OS), otherwise tests checking the text fail
        .setLocale("en-US");
  }

  public static void applyTimeouts(BrowserContext context) {
    context.setDefaultTimeout(TestConfig.actionTimeout().toMillis());
    context.setDefaultNavigationTimeout(TestConfig.navigationTimeout().toMillis());
  }

  public static Tracing.StartOptions traceOptions() {
    return new Tracing.StartOptions()
        .setScreenshots(true)
        .setSnapshots(true)
        .setSources(true);
  }

  public static BrowserContext newContext(Browser browser) {
    BrowserContext context = browser.newContext(contextOptions());
    applyTimeouts(context);
    return context;
  }
}
