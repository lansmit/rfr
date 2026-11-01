package io.student.rangiffler.launcher;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.extension.StandSchemaExtension;
import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class StandSchemaSessionListener implements LauncherSessionListener {

  private static final Path SESSION_LOG = Path.of("build", "launcher-sessions.log");

  @Override
  public void launcherSessionOpened(LauncherSession session) {
    record("opened pid=" + ProcessHandle.current().pid() + " migrationCheck=" + TestConfig.migrationCheck());
    StandSchemaExtension.check();
  }

  private static void record(String event) {
    try {
      Files.writeString(SESSION_LOG, event + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot write " + SESSION_LOG.toAbsolutePath(), exception);
    }
  }
}
