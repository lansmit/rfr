package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import io.student.rangiffler.config.TestConfig;

import java.nio.file.Paths;

public class BrowserOptions implements OptionsFactory {

  private static final String TEST_ID_ATTRIBUTE = "data-test-id";

  @Override
  public Options getOptions() {
    return new Options()
        .setBrowserName(TestConfig.browser())
        .setHeadless(TestConfig.headless())
        .setContextOptions(new Browser.NewContextOptions()
            .setViewportSize(TestConfig.viewportWidth(), TestConfig.viewportHeight())
            // keeps error messages in English on any machine (e.g. one with a Russian OS), otherwise tests checking the text fail
            .setLocale("en-US"))
        .setTestIdAttribute(TEST_ID_ATTRIBUTE)
        .setTrace(TestConfig.trace())
        .setOutputDir(Paths.get("build", "playwright"));
  }
}
