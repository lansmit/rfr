package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.student.rangiffler.config.BrowserSettings;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.nio.file.Path;

// Traces only Playwright's own context; contexts of @Session parameters are traced by SessionExtension.
// afterEach still sees an open context: Playwright closes it later, in its TestWatcher callbacks.
public class TraceExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    tracing(context).start(BrowserSettings.traceOptions().setTitle(context.getDisplayName()));
  }

  @Override
  public void afterEach(ExtensionContext context) throws IOException {
    if (!Traces.shouldKeep(context)) {
      tracing(context).stop();
      return;
    }
    Path trace = Traces.path(context, "trace.zip");
    tracing(context).stop(new Tracing.StopOptions().setPath(trace));
    Traces.attach(trace);
  }

  private static Tracing tracing(ExtensionContext context) {
    return BrowserContextExtension.getOrCreateBrowserContext(context).tracing();
  }
}
