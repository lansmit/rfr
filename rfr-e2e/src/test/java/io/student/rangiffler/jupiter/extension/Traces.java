package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.TestConfig;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Recording costs little, but a trace is worth keeping only for a failed test, so passed ones are dropped unless rfr.trace=on.
final class Traces {

  private Traces() {
  }

  private static boolean shouldKeep(ExtensionContext context) {
    return context.getExecutionException().isPresent() || TestConfig.traceAll();
  }

  private static Path path(ExtensionContext context, String fileName) {
    String testName = context.getRequiredTestClass().getName() + "." + context.getRequiredTestMethod().getName();
    return Paths.get("build", "playwright", testName + "-" + TestConfig.browser(), fileName);
  }

  private static void attach(Path trace) throws IOException {
    try (InputStream content = Files.newInputStream(trace)) {
      Allure.attachment(trace.getFileName().toString(), "application/zip", content, AttachmentOptions.withFileExtension(".zip"));
    }
  }

  static void finish(BrowserContext browserContext, ExtensionContext context, String fileName) throws IOException {
    if (!shouldKeep(context)) {
      browserContext.tracing().stop();
      return;
    }
    Path trace = path(context, fileName);
    browserContext.tracing().stop(new Tracing.StopOptions().setPath(trace));
    attach(trace);
  }
}
