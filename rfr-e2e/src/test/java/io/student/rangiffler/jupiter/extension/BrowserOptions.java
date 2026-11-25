package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import io.student.rangiffler.config.BrowserSettings;
import io.student.rangiffler.config.TestConfig;

public class BrowserOptions implements OptionsFactory {

  private static final String TEST_ID_ATTRIBUTE = "data-test-id";

  @Override
  public Options getOptions() {
    return new Options()
        .setBrowserName(TestConfig.browser())
        .setHeadless(TestConfig.headless())
        .setContextOptions(BrowserSettings.contextOptions())
        .setTestIdAttribute(TEST_ID_ATTRIBUTE);
  }
}
