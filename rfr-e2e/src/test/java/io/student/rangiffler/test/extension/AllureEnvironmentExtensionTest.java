package io.student.rangiffler.test.extension;

import io.student.rangiffler.jupiter.annotation.ProbeTarget;
import io.student.rangiffler.jupiter.extension.AllureEnvironmentExtension;
import io.student.rangiffler.jupiter.extension.ProbeTargetCondition;
import io.student.rangiffler.jupiter.extension.SuiteExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@DisplayName("AllureEnvironmentExtension")
class AllureEnvironmentExtensionTest {

  private static final Set<String> KEYS = Set.of(
      "frontUrl", "authUrl", "apiUrl", "browser", "headless", "commit", "authSchemaVersion", "apiSchemaVersion");

  private static volatile Path resultsDirectory;

  @Test
  @DisplayName("После прогона во временном каталоге результатов есть environment.properties со всеми ключами")
  void environmentFileHasAllKeys(@TempDir Path dir) throws IOException {
    resultsDirectory = dir;
    run(RecordedTarget.class);

    Path file = dir.resolve("environment.properties");
    assertTrue(Files.exists(file), "environment.properties was not written to " + dir);
    Properties properties = new Properties();
    try (Reader reader = Files.newBufferedReader(file)) {
      properties.load(reader);
    }
    assertEquals(KEYS, properties.stringPropertyNames(), "environment.properties does not have exactly the expected keys");
  }

  @Test
  @DisplayName("Файл пишется, хотя beforeSuite следующей реализации упал")
  void environmentFileIsWrittenWhenLaterBeforeSuiteFails(@TempDir Path dir) {
    resultsDirectory = dir;
    run(FailingAfterTarget.class);

    assertTrue(Files.exists(dir.resolve("environment.properties")),
        "environment.properties was not written although AllureEnvironmentExtension is registered first");
  }

  @Test
  @DisplayName("После упавшего beforeSuite JUnit не вызывает beforeAll следующих extension'ов")
  void laterExtensionIsSkippedAfterFailedBeforeSuite(@TempDir Path dir) {
    resultsDirectory = dir;
    run(FailingBeforeTarget.class);

    assertFalse(Files.exists(dir.resolve("environment.properties")),
        "AllureEnvironmentExtension registered after a failed beforeSuite still wrote its file");
  }

  private static void run(Class<?> target) {
    EngineTestKit.engine("junit-jupiter")
        .configurationParameter(ProbeTargetCondition.RUN_PROBES, "true")
        .selectors(selectClass(target))
        .execute();
  }

  static final class TemporaryResults extends AllureEnvironmentExtension {
    TemporaryResults() {
      super(resultsDirectory, AllureEnvironmentExtension::standEnvironment);
    }
  }

  static final class BrokenBefore implements SuiteExtension {
    @Override
    public void beforeSuite(ExtensionContext context) {
      throw new IllegalStateException("beforeSuite of BrokenBefore fails on purpose");
    }
  }

  @ProbeTarget
  @Tag("ui")
  @ExtendWith(TemporaryResults.class)
  static final class RecordedTarget {
    @Test
    void runs() {
    }
  }

  @ProbeTarget
  @Tag("ui")
  @ExtendWith({TemporaryResults.class, BrokenBefore.class})
  static final class FailingAfterTarget {
    @Test
    void runs() {
    }
  }

  @ProbeTarget
  @Tag("ui")
  @ExtendWith({BrokenBefore.class, TemporaryResults.class})
  static final class FailingBeforeTarget {
    @Test
    void runs() {
    }
  }
}
