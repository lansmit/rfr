package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import io.student.rangiffler.config.TestConfig;
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
final class Traces {

  // A folder per run, so a rerun of the suite doesn't overwrite the traces of the previous one.
  private static final String RUN = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

  private Traces() {
  }

  private static boolean shouldKeep(ExtensionContext context) {
    boolean failed = false;
    if (context.getExecutionException().isPresent()) {
      Throwable exception = context.getExecutionException().get();
      if (!(exception instanceof TestAbortedException)) {
        failed = true;
      }
    }
    return failed || TestConfig.traceAll();
  }

  private static Path path(ExtensionContext context, String fileName) {
    String testName = context.getRequiredTestClass().getName() + "." + context.getRequiredTestMethod().getName();
    String folder = testName + "-" + TestConfig.browser();
    String uniqueId = context.getUniqueId();
    int numberStart = uniqueId.lastIndexOf('#');
    if (numberStart != -1) {
      folder = folder + "-" + uniqueId.substring(numberStart + 1, uniqueId.length() - 1);
    }
    return Paths.get("build", "playwright", RUN, folder, fileName);
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
    Files.createDirectories(trace.getParent());
    browserContext.tracing().stop(new Tracing.StopOptions().setPath(trace));
    attach(trace);
  }
}
