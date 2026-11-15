package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Recording costs little, but a trace is worth keeping only for a failed test, so passed ones are dropped unless rfr.trace=on.
// afterEach still sees an open context: Playwright closes it later, in its TestWatcher callbacks.
public class TraceExtension implements BeforeEachCallback, AfterEachCallback {

  @Override
  public void beforeEach(ExtensionContext context) {
    tracing(context).start(new Tracing.StartOptions()
        .setTitle(context.getDisplayName())
        .setScreenshots(true)
        .setSnapshots(true)
        .setSources(true));
  }

  @Override
  public void afterEach(ExtensionContext context) throws IOException {
    boolean failed = context.getExecutionException().isPresent();
    if (!failed && !TestConfig.traceAll()) {
      tracing(context).stop();
      return;
    }
    Path trace = tracePath(context);
    tracing(context).stop(new Tracing.StopOptions().setPath(trace));
    try (InputStream content = Files.newInputStream(trace)) {
      Allure.attachment("trace.zip", "application/zip", content, AttachmentOptions.withFileExtension(".zip"));
    }
  }

  private static Tracing tracing(ExtensionContext context) {
    return BrowserContextExtension.getOrCreateBrowserContext(context).tracing();
  }

  private static Path tracePath(ExtensionContext context) {
    String testName = context.getRequiredTestClass().getName() + "." + context.getRequiredTestMethod().getName();
    return Paths.get("build", "playwright", testName + "-" + TestConfig.browser(), "trace.zip");
  }
}
