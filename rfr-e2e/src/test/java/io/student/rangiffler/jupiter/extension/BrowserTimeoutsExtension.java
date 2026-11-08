package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

// Options of @UsePlaywright have no timeouts, so they are set here.
// The context is only reachable through Playwright's internal impl class: may break on upgrade.
public class BrowserTimeoutsExtension implements BeforeEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    BrowserContext browserContext = BrowserContextExtension.getOrCreateBrowserContext(context);
    browserContext.setDefaultTimeout(TestConfig.actionTimeout().toMillis());
    browserContext.setDefaultNavigationTimeout(TestConfig.navigationTimeout().toMillis());
  }
}
