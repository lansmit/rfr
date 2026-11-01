package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Migrations;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class AllureEnvironmentExtension implements SuiteExtension {

  private static final String UI_TAG = "ui";
  private static final String FILE_NAME = "environment.properties";
  private static final String UNAVAILABLE = "unavailable";

  private final Path resultsDirectory;
  private final Supplier<Map<String, String>> environment;

  public AllureEnvironmentExtension() {
    this(Path.of(System.getProperty("allure.results.directory", "build/allure-results")),
        AllureEnvironmentExtension::standEnvironment);
  }

  protected AllureEnvironmentExtension(Path resultsDirectory, Supplier<Map<String, String>> environment) {
    this.resultsDirectory = resultsDirectory;
    this.environment = environment;
  }

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    if (context.getTags().contains(UI_TAG)) {
      SuiteExtension.super.beforeAll(context);
    }
  }

  @Override
  public void afterSuite(ExtensionContext context) {
    String content = environment.get().entrySet().stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(System.lineSeparator(), "", System.lineSeparator()));
    try {
      Files.createDirectories(resultsDirectory);
      Files.writeString(resultsDirectory.resolve(FILE_NAME), content);
    } catch (IOException exception) {
      throw new UncheckedIOException("Cannot write " + resultsDirectory.resolve(FILE_NAME).toAbsolutePath(), exception);
    }
  }

  protected static Map<String, String> standEnvironment() {
    Map<String, String> environment = new LinkedHashMap<>();
    environment.put("frontUrl", TestConfig.frontUrl());
    environment.put("authUrl", TestConfig.authUrl());
    environment.put("apiUrl", TestConfig.apiUrl());
    environment.put("browser", TestConfig.browser());
    environment.put("headless", String.valueOf(TestConfig.headless()));
    environment.put("commit", TestConfig.commit());
    environment.put("authSchemaVersion", schemaVersion(new Migrations(Databases.AUTH, TestConfig.authMigrations())));
    environment.put("apiSchemaVersion", schemaVersion(new Migrations(Databases.API, TestConfig.apiMigrations())));
    return environment;
  }

  private static String schemaVersion(Migrations migrations) {
    return migrations.currentVersion().orElse(UNAVAILABLE);
  }
}
