pfdpackage io.student.rangiffler.test;

import io.student.rangiffler.jupiter.extension.StandSchemaExtension;
import org.junit.platform.suite.api.BeforeSuite;
import org.junit.platform.suite.api.IncludeTags;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("UI tests")
@SelectPackages("io.student.rangiffler.test")
@IncludeTags("ui")
public class UiSuite {

  @BeforeSuite
  static void checkMigrations() {
    StandSchemaExtension.check();
  }
}
