package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// Recording costs little, but a trace is worth keeping only for a failed test, so passed ones are dropped unless rfr.trace=on.
// afterEach still sees an open context: Playwright closes it later, in its TestWatcher callbacks.
public class TraceExtension implements BeforeEachCallback, AfterEachCallback {

  // A folder per run, so a rerun of the suite doesn't overwrite the traces of the previous one.
  private static final String RUN = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

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
    boolean failed = false;
    if (context.getExecutionException().isPresent()) {
      Throwable exception = context.getExecutionException().get();
      if (!(exception instanceof TestAbortedException)) {
        failed = true;
      }
    }
    if (!failed && !TestConfig.traceAll()) {
      tracing(context).stop();
      return;
    }
    Path trace = tracePath(context);
    Files.createDirectories(trace.getParent());
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
    String folder = testName + "-" + TestConfig.browser();
    String uniqueId = context.getUniqueId();
    int numberStart = uniqueId.lastIndexOf('#');
    if (numberStart != -1) {
      folder = folder + "-" + uniqueId.substring(numberStart + 1, uniqueId.length() - 1);
    }
    return Paths.get("build", "playwright", RUN, folder, "trace.zip");
  }
}
