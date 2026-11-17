package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.student.rangiffler.config.BrowserSettings;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;

// Traces only Playwright's own context; contexts of @Session parameters are traced by SessionExtension.
// afterEach still sees an open context: Playwright closes it later, in its TestWatcher callbacks.
public class TraceExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    BrowserSettings.startTracing(BrowserContextExtension.getOrCreateBrowserContext(context), context.getDisplayName());
  }

  @Override
  public void afterEach(ExtensionContext context) throws IOException {
    Traces.finish(BrowserContextExtension.getOrCreateBrowserContext(context), context, "trace.zip");
  }
}
